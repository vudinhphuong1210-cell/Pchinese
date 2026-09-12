package net.pchinese.aiops.application;

import net.pchinese.aiops.domain.AiCapability;
import net.pchinese.aiops.domain.OperationalMeasurementOutcome;
import net.pchinese.aiops.persistence.AiOperationalMeasurementEntity;
import net.pchinese.aiops.persistence.AiOperationalMeasurementRepository;
import net.pchinese.common.error.ApiException;
import net.pchinese.entitlement.domain.PlanCode;
import net.pchinese.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiUsageReportService {
    private static final Duration MAX_RANGE = Duration.ofDays(50);
    private final AiOperationalMeasurementRepository measurements;

    public AiUsageReportService(AiOperationalMeasurementRepository measurements) {
        this.measurements = measurements;
    }

    @Transactional(readOnly = true)
    public UsageReportView report(UserPrincipal actor, ReportQuery query) {
        requireAdmin(actor);
        validateRange(query.fromInclusive(), query.toExclusive());
        List<AiOperationalMeasurementEntity> selected = select(query);
        return toView(query.fromInclusive(), query.toExclusive(), selected,
                measurements.existsPendingBefore(query.toExclusive()));
    }

    @Transactional(readOnly = true)
    public AggregateWindow aggregateWindow(Instant fromInclusive, Instant toExclusive, PlanCode planCode,
                                           UUID policyVersionId, AiCapability capability) {
        List<AiOperationalMeasurementEntity> selected = select(new ReportQuery(fromInclusive, toExclusive, planCode,
                policyVersionId, capability));
        return new AggregateWindow(selected, measurements.existsPendingBefore(toExclusive));
    }

    private List<AiOperationalMeasurementEntity> select(ReportQuery query) {
        return measurements.findFinalizedBetween(query.fromInclusive(), query.toExclusive()).stream()
                .filter(measurement -> query.planCode() == null || measurement.getPolicyVersion().getSubscriptionPlan().getPlanCode() == query.planCode())
                .filter(measurement -> query.policyVersionId() == null || measurement.getPolicyVersion().getId().equals(query.policyVersionId()))
                .filter(measurement -> query.capability() == null || measurement.getCapability() == query.capability())
                .toList();
    }

    private UsageReportView toView(Instant fromInclusive, Instant toExclusive,
                                   List<AiOperationalMeasurementEntity> selected, boolean partialData) {
        Map<BucketKey, BucketAccumulator> buckets = new LinkedHashMap<>();
        Instant measuredThrough = null;
        for (AiOperationalMeasurementEntity measurement : selected) {
            BucketKey key = new BucketKey(measurement.getPolicyVersion().getSubscriptionPlan().getPlanCode(),
                    measurement.getPolicyVersion().getId(), measurement.getCapability(), measurement.getOutcome());
            buckets.computeIfAbsent(key, unused -> new BucketAccumulator(key)).add(measurement);
            if (measuredThrough == null || measurement.getFinalizedAt().isAfter(measuredThrough)) {
                measuredThrough = measurement.getFinalizedAt();
            }
        }
        List<UsageBucketView> result = buckets.values().stream().map(BucketAccumulator::view)
                .sorted(Comparator.comparing((UsageBucketView bucket) -> bucket.planCode().name())
                        .thenComparing(bucket -> bucket.capability().name())
                        .thenComparing(bucket -> bucket.outcome().name()))
                .toList();
        return new UsageReportView(fromInclusive, toExclusive, measuredThrough == null ? Instant.now() : measuredThrough,
                partialData, result);
    }

    private void validateRange(Instant fromInclusive, Instant toExclusive) {
        Instant now = Instant.now();
        if (fromInclusive == null || toExclusive == null || !toExclusive.isAfter(fromInclusive)
                || Duration.between(fromInclusive, toExclusive).compareTo(MAX_RANGE) > 0
                || fromInclusive.isBefore(now.minus(4, ChronoUnit.MONTHS)) || toExclusive.isAfter(now.plus(1, ChronoUnit.MINUTES))) {
            throw ApiException.validation("The report range must be within the latest four months and no longer than 50 days.");
        }
    }

    private void requireAdmin(UserPrincipal actor) {
        if (actor == null) throw ApiException.unauthenticated();
        if (!actor.isAdmin()) throw ApiException.forbidden();
    }

    private record BucketKey(PlanCode planCode, UUID policyVersionId, AiCapability capability,
                             OperationalMeasurementOutcome outcome) { }

    private static class BucketAccumulator {
        private final BucketKey key;
        private int requestCount;
        private int reservedUnits;
        private int usedUnits;
        private int refundedUnits;
        private long reportedMeteringSamples;
        private long unavailableMeteringSamples;
        private Long inputTokens;
        private Long outputTokens;
        private Long totalTokens;
        private long responseTimeSamples;
        private BigDecimal responseTimeSum = BigDecimal.ZERO;
        private final Map<String, BigDecimal> costs = new LinkedHashMap<>();

        private BucketAccumulator(BucketKey key) { this.key = key; }

        private void add(AiOperationalMeasurementEntity measurement) {
            requestCount++;
            reservedUnits += measurement.getReservedUnits();
            usedUnits += measurement.getUsedUnits();
            refundedUnits += measurement.getRefundedUnits();
            boolean hasMetering = measurement.getInputTokens() != null || measurement.getOutputTokens() != null
                    || measurement.getTotalTokens() != null || measurement.getEstimatedCost() != null;
            if (hasMetering) reportedMeteringSamples++; else unavailableMeteringSamples++;
            inputTokens = sumNullable(inputTokens, measurement.getInputTokens());
            outputTokens = sumNullable(outputTokens, measurement.getOutputTokens());
            totalTokens = sumNullable(totalTokens, measurement.getTotalTokens());
            if (measurement.getEstimatedCost() != null) {
                costs.merge(measurement.getCostCurrency(), measurement.getEstimatedCost(), BigDecimal::add);
            }
            if (measurement.getEndToEndDurationMs() != null) {
                responseTimeSamples++;
                responseTimeSum = responseTimeSum.add(BigDecimal.valueOf(measurement.getEndToEndDurationMs()));
            }
        }

        private UsageBucketView view() {
            List<CostByCurrencyView> costByCurrency = costs.entrySet().stream()
                    .map(entry -> new CostByCurrencyView(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparing(CostByCurrencyView::currency)).toList();
            BigDecimal average = responseTimeSamples == 0 ? null
                    : responseTimeSum.divide(BigDecimal.valueOf(responseTimeSamples), 2, RoundingMode.HALF_UP);
            return new UsageBucketView(key.planCode(), key.policyVersionId(), key.capability(), key.outcome(), requestCount,
                    reservedUnits, usedUnits, refundedUnits,
                    new ProviderMeteringView(reportedMeteringSamples, unavailableMeteringSamples, inputTokens,
                            outputTokens, totalTokens, costByCurrency),
                    new ResponseTimeView(responseTimeSamples, average));
        }

        private Long sumNullable(Long existing, Long next) {
            if (next == null) return existing;
            return existing == null ? next : existing + next;
        }
    }

    public record ReportQuery(Instant fromInclusive, Instant toExclusive, PlanCode planCode,
                              UUID policyVersionId, AiCapability capability) { }
    public record UsageReportView(Instant fromInclusive, Instant toExclusive, Instant measuredThroughAt,
                                  boolean partialData, List<UsageBucketView> buckets) { }
    public record UsageBucketView(PlanCode planCode, UUID policyVersionId, AiCapability capability,
                                  OperationalMeasurementOutcome outcome, int requestCount, int reservedUnits,
                                  int usedUnits, int refundedUnits, ProviderMeteringView providerMetering,
                                  ResponseTimeView responseTime) { }
    public record ProviderMeteringView(long reportedSampleCount, long unavailableSampleCount, Long inputTokens,
                                       Long outputTokens, Long totalTokens, List<CostByCurrencyView> costByCurrency) { }
    public record CostByCurrencyView(String currency, BigDecimal amount) { }
    public record ResponseTimeView(long sampleCount, BigDecimal averageMilliseconds) { }
    public record AggregateWindow(List<AiOperationalMeasurementEntity> measurements, boolean partialData) { }
}
