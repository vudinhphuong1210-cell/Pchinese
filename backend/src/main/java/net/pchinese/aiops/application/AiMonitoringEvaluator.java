package net.pchinese.aiops.application;

import net.pchinese.aiops.domain.EvaluationWindow;
import net.pchinese.aiops.domain.MonitoringMetric;
import net.pchinese.aiops.persistence.AiMonitoringRuleEntity;
import net.pchinese.aiops.persistence.AiMonitoringRuleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class AiMonitoringEvaluator {
    private final AiMonitoringRuleRepository rules;
    private final AiUsageReportService reports;
    private final AiMonitoringAlertService alerts;

    public AiMonitoringEvaluator(AiMonitoringRuleRepository rules, AiUsageReportService reports,
                                 AiMonitoringAlertService alerts) {
        this.rules = rules;
        this.reports = reports;
        this.alerts = alerts;
    }

    @Transactional
    public void evaluateAll() {
        Instant now = Instant.now();
        for (AiMonitoringRuleEntity rule : rules.findByEnabledTrue()) {
            evaluate(rule, now);
        }
    }

    private void evaluate(AiMonitoringRuleEntity rule, Instant now) {
        Instant from = rule.getEvaluationWindow() == EvaluationWindow.ONE_HOUR
                ? now.minus(1, ChronoUnit.HOURS) : now.minus(24, ChronoUnit.HOURS);
        var aggregate = reports.aggregateWindow(from, now,
                rule.getSubscriptionPlan() == null ? null : rule.getSubscriptionPlan().getPlanCode(),
                rule.getPolicyVersion() == null ? null : rule.getPolicyVersion().getId(), rule.getCapability());
        Evaluation evaluation = metric(rule.getMetric(), aggregate.measurements(), aggregate.partialData());
        alerts.applyEvaluation(rule, evaluation.value(), evaluation.partialData(),
                !evaluation.partialData() && evaluation.value().compareTo(rule.getThreshold()) > 0, now);
    }

    private Evaluation metric(MonitoringMetric metric, List<net.pchinese.aiops.persistence.AiOperationalMeasurementEntity> data,
                              boolean pendingData) {
        if (metric == MonitoringMetric.REQUEST_VOLUME) return new Evaluation(BigDecimal.valueOf(data.size()), pendingData);
        if (metric == MonitoringMetric.QUOTA_DENIAL_RATE) {
            if (data.isEmpty()) return new Evaluation(BigDecimal.ZERO, pendingData);
            long denied = data.stream().filter(item -> item.getOutcome().name().equals("QUOTA_DENIED")).count();
            return new Evaluation(percent(denied, data.size()), pendingData);
        }
        if (metric == MonitoringMetric.FAILURE_RATE) {
            if (data.isEmpty()) return new Evaluation(BigDecimal.ZERO, pendingData);
            long failed = data.stream().filter(item -> item.getOutcome().name().startsWith("FAILED")).count();
            return new Evaluation(percent(failed, data.size()), pendingData);
        }
        if (metric == MonitoringMetric.TOKEN_VOLUME) {
            boolean unavailable = data.stream().anyMatch(item -> item.getTotalTokens() == null);
            long total = data.stream().filter(item -> item.getTotalTokens() != null).mapToLong(item -> item.getTotalTokens()).sum();
            return new Evaluation(BigDecimal.valueOf(total), pendingData || unavailable);
        }
        if (metric == MonitoringMetric.RESPONSE_TIME) {
            boolean unavailable = data.stream().anyMatch(item -> item.getEndToEndDurationMs() == null);
            List<Long> times = data.stream().map(item -> item.getEndToEndDurationMs()).filter(java.util.Objects::nonNull).toList();
            BigDecimal value = times.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(times.stream().mapToLong(Long::longValue).average().orElse(0));
            return new Evaluation(value, pendingData || unavailable);
        }
        // Cost values cannot be converted across currencies. A mixed or unavailable window remains
        // explicitly partial instead of comparing a fabricated aggregate to the threshold.
        var currencies = data.stream().filter(item -> item.getEstimatedCost() != null).map(item -> item.getCostCurrency()).distinct().toList();
        boolean unavailable = data.stream().anyMatch(item -> item.getEstimatedCost() == null) || currencies.size() > 1;
        BigDecimal cost = data.stream().filter(item -> item.getEstimatedCost() != null)
                .map(item -> item.getEstimatedCost()).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Evaluation(cost, pendingData || unavailable);
    }

    private BigDecimal percent(long numerator, long denominator) {
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private record Evaluation(BigDecimal value, boolean partialData) { }
}
