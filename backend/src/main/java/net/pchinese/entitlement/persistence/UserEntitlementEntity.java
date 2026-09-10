package net.pchinese.entitlement.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import net.pchinese.entitlement.domain.EntitlementSourceType;
import net.pchinese.entitlement.domain.EntitlementStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "user_entitlements")
public class UserEntitlementEntity {
    @Id
    @Column(name = "user_entitlement_id", nullable = false, updatable = false)
    private UUID userEntitlementId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id", nullable = false)
    private SubscriptionPlanEntity subscriptionPlan;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private EntitlementStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private EntitlementSourceType sourceType;

    @Column(name = "external_reference_ciphertext")
    private byte[] externalReferenceCiphertext;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "ai_quota_period_started_at", nullable = false)
    private Instant aiQuotaPeriodStartedAt;

    @Column(name = "ai_used_units", nullable = false)
    private int aiUsedUnits;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected UserEntitlementEntity() { }

    public static UserEntitlementEntity createDefaultFree(UUID userId, SubscriptionPlanEntity freePlan, Instant now) {
        UserEntitlementEntity entitlement = new UserEntitlementEntity();
        entitlement.userEntitlementId = UUID.randomUUID();
        entitlement.userId = userId;
        entitlement.subscriptionPlan = freePlan;
        entitlement.status = EntitlementStatus.ACTIVE;
        entitlement.sourceType = EntitlementSourceType.DEFAULT;
        entitlement.startsAt = now;
        entitlement.endsAt = null;
        entitlement.aiQuotaPeriodStartedAt = now;
        entitlement.aiUsedUnits = 0;
        entitlement.createdAt = now;
        entitlement.updatedAt = now;
        return entitlement;
    }

    public UUID getUserEntitlementId() { return userEntitlementId; }
    public UUID getUserId() { return userId; }
    public SubscriptionPlanEntity getSubscriptionPlan() { return subscriptionPlan; }
    public EntitlementStatus getStatus() { return status; }
    public EntitlementSourceType getSourceType() { return sourceType; }
    public byte[] getExternalReferenceCiphertext() { return externalReferenceCiphertext; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public Instant getAiQuotaPeriodStartedAt() { return aiQuotaPeriodStartedAt; }
    public int getAiUsedUnits() { return aiUsedUnits; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    public boolean isExpired(Instant now) {
        return endsAt != null && !now.isBefore(endsAt);
    }

    public boolean isActive(Instant now) {
        return status == EntitlementStatus.ACTIVE && !isExpired(now);
    }

    public boolean rollCycleIfExpired(Instant now, int periodDays) {
        Instant cycleEnd = aiQuotaPeriodStartedAt.plus(periodDays, ChronoUnit.DAYS);
        if (!now.isBefore(cycleEnd)) {
            this.aiQuotaPeriodStartedAt = now;
            this.aiUsedUnits = 0;
            this.updatedAt = now;
            return true;
        }
        return false;
    }

    public void incrementUsedUnits(int units, Instant now) {
        this.aiUsedUnits += units;
        this.updatedAt = now;
    }

    public void refundUsedUnits(int units, Instant now) {
        this.aiUsedUnits = Math.max(0, this.aiUsedUnits - units);
        this.updatedAt = now;
    }
}
