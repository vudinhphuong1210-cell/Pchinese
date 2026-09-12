package net.pchinese.allowance.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import net.pchinese.allowance.domain.AllowanceEventStatus;
import net.pchinese.allowance.domain.AllowanceFeatureType;
import net.pchinese.aiops.persistence.PlanPolicyVersionEntity;
import net.pchinese.entitlement.persistence.EntitlementAllowanceCycleEntity;
import net.pchinese.entitlement.persistence.UserEntitlementEntity;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_usage_events")
public class AiUsageEventEntity {
    @Id
    @Column(name = "ai_usage_event_id", nullable = false, updatable = false)
    private UUID aiUsageEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_entitlement_id", nullable = false)
    private UserEntitlementEntity userEntitlement;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entitlement_allowance_cycle_id", nullable = false)
    private EntitlementAllowanceCycleEntity allowanceCycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_policy_version_id", nullable = false)
    private PlanPolicyVersionEntity policyVersion;

    @Column(name = "client_request_id", nullable = false)
    private UUID clientRequestId;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "request_fingerprint_hash", nullable = false, length = 64)
    private String requestFingerprintHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_type", nullable = false, length = 32)
    private AllowanceFeatureType featureType;

    @Column(name = "requested_units", nullable = false)
    private short requestedUnits;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private AllowanceEventStatus status;

    @Column(name = "ai_service_request_reference", length = 128)
    private String aiServiceRequestReference;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected AiUsageEventEntity() { }

    public static AiUsageEventEntity createReserved(UserEntitlementEntity userEntitlement,
                                                   EntitlementAllowanceCycleEntity allowanceCycle,
                                                   UUID userId, UUID clientRequestId,
                                                   String fingerprintHash, AllowanceFeatureType featureType, short requestedUnits, Instant now) {
        AiUsageEventEntity event = new AiUsageEventEntity();
        event.aiUsageEventId = UUID.randomUUID();
        event.userEntitlement = userEntitlement;
        event.allowanceCycle = allowanceCycle;
        event.policyVersion = allowanceCycle.getPolicyVersion();
        event.userId = userId;
        event.clientRequestId = clientRequestId;
        event.requestFingerprintHash = fingerprintHash;
        event.featureType = featureType;
        event.requestedUnits = requestedUnits;
        event.status = AllowanceEventStatus.RESERVED;
        event.createdAt = now;
        return event;
    }

    /** Test-only compatibility factory for pre-F12 unit fixtures. Persisted events bind a cycle and policy. */
    public static AiUsageEventEntity createReserved(UserEntitlementEntity userEntitlement, UUID userId, UUID clientRequestId,
                                                   String fingerprintHash, AllowanceFeatureType featureType, short requestedUnits, Instant now) {
        AiUsageEventEntity event = new AiUsageEventEntity();
        event.aiUsageEventId = UUID.randomUUID();
        event.userEntitlement = userEntitlement;
        event.userId = userId;
        event.clientRequestId = clientRequestId;
        event.requestFingerprintHash = fingerprintHash;
        event.featureType = featureType;
        event.requestedUnits = requestedUnits;
        event.status = AllowanceEventStatus.RESERVED;
        event.createdAt = now;
        return event;
    }

    public UUID getAiUsageEventId() { return aiUsageEventId; }
    public UserEntitlementEntity getUserEntitlement() { return userEntitlement; }
    public UUID getUserId() { return userId; }
    public EntitlementAllowanceCycleEntity getAllowanceCycle() { return allowanceCycle; }
    public PlanPolicyVersionEntity getPolicyVersion() { return policyVersion; }
    public UUID getClientRequestId() { return clientRequestId; }
    public String getRequestFingerprintHash() { return requestFingerprintHash; }
    public AllowanceFeatureType getFeatureType() { return featureType; }
    public short getRequestedUnits() { return requestedUnits; }
    public AllowanceEventStatus getStatus() { return status; }
    public String getAiServiceRequestReference() { return aiServiceRequestReference; }
    public String getFailureCode() { return failureCode; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }

    public void markSucceeded(String reference, Instant now) {
        this.status = AllowanceEventStatus.SUCCEEDED;
        this.aiServiceRequestReference = reference;
        this.completedAt = now;
    }

    public void markFailedRefunded(String failureCode, Instant now) {
        this.status = AllowanceEventStatus.FAILED_REFUNDED;
        this.failureCode = failureCode;
        this.completedAt = now;
    }

    public void markFailedConsumed(String failureCode, Instant now) {
        this.status = AllowanceEventStatus.FAILED_CONSUMED;
        this.failureCode = failureCode;
        this.completedAt = now;
    }
}
