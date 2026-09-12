package net.pchinese.aiops.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import net.pchinese.aiops.domain.AiCapability;
import net.pchinese.aiops.domain.OperationalMeasurementOutcome;
import net.pchinese.aiops.domain.ProviderTelemetry;
import net.pchinese.allowance.persistence.AiUsageEventEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_operational_measurements")
public class AiOperationalMeasurementEntity {
    @Id
    @Column(name = "ai_operational_measurement_id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "measurement_key", nullable = false, unique = true, length = 64)
    private String measurementKey;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_usage_event_id")
    private AiUsageEventEntity usageEvent;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_policy_version_id", nullable = false)
    private PlanPolicyVersionEntity policyVersion;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AiCapability capability;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OperationalMeasurementOutcome outcome;
    @Column(name = "reserved_units", nullable = false)
    private int reservedUnits;
    @Column(name = "used_units", nullable = false)
    private int usedUnits;
    @Column(name = "refunded_units", nullable = false)
    private int refundedUnits;
    @Column(name = "input_tokens") private Long inputTokens;
    @Column(name = "output_tokens") private Long outputTokens;
    @Column(name = "total_tokens") private Long totalTokens;
    @Column(name = "estimated_cost", precision = 19, scale = 6) private BigDecimal estimatedCost;
    @Column(name = "cost_currency", length = 3) private String costCurrency;
    @Column(name = "provider_duration_ms") private Long providerDurationMs;
    @Column(name = "end_to_end_duration_ms") private Long endToEndDurationMs;
    @Column(name = "failure_class", length = 32) private String failureClass;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "finalized_at") private Instant finalizedAt;

    protected AiOperationalMeasurementEntity() { }

    public static AiOperationalMeasurementEntity pending(String measurementKey, AiUsageEventEntity event, Instant now) {
        AiOperationalMeasurementEntity measurement = new AiOperationalMeasurementEntity();
        measurement.id = UUID.randomUUID();
        measurement.measurementKey = measurementKey;
        measurement.usageEvent = event;
        measurement.policyVersion = event.getPolicyVersion();
        measurement.capability = AiCapability.valueOf(event.getFeatureType().name());
        measurement.outcome = OperationalMeasurementOutcome.PENDING;
        measurement.reservedUnits = event.getRequestedUnits();
        measurement.occurredAt = now;
        return measurement;
    }

    public static AiOperationalMeasurementEntity quotaDenied(String measurementKey, PlanPolicyVersionEntity policy,
                                                               AiCapability capability, Instant now) {
        AiOperationalMeasurementEntity measurement = new AiOperationalMeasurementEntity();
        measurement.id = UUID.randomUUID();
        measurement.measurementKey = measurementKey;
        measurement.policyVersion = policy;
        measurement.capability = capability;
        measurement.outcome = OperationalMeasurementOutcome.QUOTA_DENIED;
        measurement.occurredAt = now;
        measurement.finalizedAt = now;
        return measurement;
    }

    public void finalizeOutcome(OperationalMeasurementOutcome finalOutcome, ProviderTelemetry telemetry,
                                long endToEndDurationMs, Instant now) {
        if (outcome.isFinal()) return;
        this.outcome = finalOutcome;
        this.usedUnits = finalOutcome == OperationalMeasurementOutcome.SUCCEEDED ? reservedUnits : 0;
        this.refundedUnits = finalOutcome == OperationalMeasurementOutcome.FAILED_REFUNDED ? reservedUnits : 0;
        this.inputTokens = telemetry.inputTokens();
        this.outputTokens = telemetry.outputTokens();
        this.totalTokens = telemetry.totalTokens();
        this.estimatedCost = telemetry.estimatedCost();
        this.costCurrency = telemetry.costCurrency();
        this.providerDurationMs = telemetry.providerDurationMs();
        this.failureClass = telemetry.failureClass();
        this.endToEndDurationMs = Math.max(0, endToEndDurationMs);
        this.finalizedAt = now;
    }

    public UUID getId() { return id; }
    public String getMeasurementKey() { return measurementKey; }
    public AiUsageEventEntity getUsageEvent() { return usageEvent; }
    public PlanPolicyVersionEntity getPolicyVersion() { return policyVersion; }
    public AiCapability getCapability() { return capability; }
    public OperationalMeasurementOutcome getOutcome() { return outcome; }
    public int getReservedUnits() { return reservedUnits; }
    public int getUsedUnits() { return usedUnits; }
    public int getRefundedUnits() { return refundedUnits; }
    public Long getInputTokens() { return inputTokens; }
    public Long getOutputTokens() { return outputTokens; }
    public Long getTotalTokens() { return totalTokens; }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public String getCostCurrency() { return costCurrency; }
    public Long getProviderDurationMs() { return providerDurationMs; }
    public Long getEndToEndDurationMs() { return endToEndDurationMs; }
    public String getFailureClass() { return failureClass; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getFinalizedAt() { return finalizedAt; }
}
