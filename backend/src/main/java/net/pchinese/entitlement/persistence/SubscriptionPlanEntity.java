package net.pchinese.entitlement.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import net.pchinese.entitlement.domain.PlanCode;
import net.pchinese.entitlement.domain.PlanStatus;
import net.pchinese.entitlement.domain.QuotaPeriod;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlanEntity {
    @Id
    @Column(name = "subscription_plan_id", nullable = false, updatable = false)
    private UUID subscriptionPlanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_code", nullable = false, unique = true, length = 32)
    private PlanCode planCode;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "max_active_sessions", nullable = false)
    private short maxActiveSessions;

    @Column(name = "ai_quota_units", nullable = false)
    private int aiQuotaUnits;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_quota_period", nullable = false, length = 16)
    private QuotaPeriod aiQuotaPeriod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PlanStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SubscriptionPlanEntity() { }

    public static SubscriptionPlanEntity create(PlanCode planCode, String displayName, short maxActiveSessions,
                                                int aiQuotaUnits, QuotaPeriod aiQuotaPeriod, PlanStatus status, Instant now) {
        SubscriptionPlanEntity plan = new SubscriptionPlanEntity();
        plan.subscriptionPlanId = UUID.randomUUID();
        plan.planCode = planCode;
        plan.displayName = displayName;
        plan.maxActiveSessions = maxActiveSessions;
        plan.aiQuotaUnits = aiQuotaUnits;
        plan.aiQuotaPeriod = aiQuotaPeriod;
        plan.status = status;
        plan.createdAt = now;
        plan.updatedAt = now;
        return plan;
    }

    public UUID getSubscriptionPlanId() { return subscriptionPlanId; }
    public PlanCode getPlanCode() { return planCode; }
    public String getDisplayName() { return displayName; }
    public short getMaxActiveSessions() { return maxActiveSessions; }
    public int getAiQuotaUnits() { return aiQuotaUnits; }
    public QuotaPeriod getAiQuotaPeriod() { return aiQuotaPeriod; }
    public PlanStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
