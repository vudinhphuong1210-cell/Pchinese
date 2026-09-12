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
import jakarta.persistence.Version;
import net.pchinese.aiops.domain.AiCapability;
import net.pchinese.aiops.domain.EvaluationWindow;
import net.pchinese.aiops.domain.MonitoringMetric;
import net.pchinese.entitlement.persistence.SubscriptionPlanEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_monitoring_rules")
public class AiMonitoringRuleEntity {
    @Id
    @Column(name = "ai_monitoring_rule_id", nullable = false, updatable = false)
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MonitoringMetric metric;
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal threshold;
    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_window", nullable = false, length = 16)
    private EvaluationWindow evaluationWindow;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id")
    private SubscriptionPlanEntity subscriptionPlan;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_policy_version_id")
    private PlanPolicyVersionEntity policyVersion;
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private AiCapability capability;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;
    @Column(name = "updated_by_user_id", nullable = false)
    private UUID updatedByUserId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    protected AiMonitoringRuleEntity() { }

    public static AiMonitoringRuleEntity create(MonitoringMetric metric, BigDecimal threshold,
                                                EvaluationWindow evaluationWindow,
                                                SubscriptionPlanEntity subscriptionPlan,
                                                PlanPolicyVersionEntity policyVersion,
                                                AiCapability capability, boolean enabled,
                                                UUID actorId, Instant now) {
        AiMonitoringRuleEntity rule = new AiMonitoringRuleEntity();
        rule.id = UUID.randomUUID();
        rule.metric = metric;
        rule.threshold = threshold;
        rule.evaluationWindow = evaluationWindow;
        rule.subscriptionPlan = subscriptionPlan;
        rule.policyVersion = policyVersion;
        rule.capability = capability;
        rule.enabled = enabled;
        rule.createdByUserId = actorId;
        rule.updatedByUserId = actorId;
        rule.createdAt = now;
        rule.updatedAt = now;
        return rule;
    }

    public void update(MonitoringMetric metric, BigDecimal threshold, EvaluationWindow evaluationWindow,
                       SubscriptionPlanEntity subscriptionPlan, PlanPolicyVersionEntity policyVersion,
                       AiCapability capability, boolean enabled, UUID actorId, Instant now) {
        this.metric = metric;
        this.threshold = threshold;
        this.evaluationWindow = evaluationWindow;
        this.subscriptionPlan = subscriptionPlan;
        this.policyVersion = policyVersion;
        this.capability = capability;
        this.enabled = enabled;
        this.updatedByUserId = actorId;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public MonitoringMetric getMetric() { return metric; }
    public BigDecimal getThreshold() { return threshold; }
    public EvaluationWindow getEvaluationWindow() { return evaluationWindow; }
    public SubscriptionPlanEntity getSubscriptionPlan() { return subscriptionPlan; }
    public PlanPolicyVersionEntity getPolicyVersion() { return policyVersion; }
    public AiCapability getCapability() { return capability; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
