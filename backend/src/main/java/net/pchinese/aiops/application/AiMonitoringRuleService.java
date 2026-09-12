package net.pchinese.aiops.application;

import net.pchinese.aiops.domain.AiAdminAuditEventType;
import net.pchinese.aiops.domain.AiAdminAuditTargetType;
import net.pchinese.aiops.domain.AiCapability;
import net.pchinese.aiops.domain.EvaluationWindow;
import net.pchinese.aiops.domain.MonitoringMetric;
import net.pchinese.aiops.persistence.AiMonitoringRuleEntity;
import net.pchinese.aiops.persistence.AiMonitoringRuleRepository;
import net.pchinese.aiops.persistence.PlanPolicyVersionEntity;
import net.pchinese.aiops.persistence.PlanPolicyVersionRepository;
import net.pchinese.common.error.ApiException;
import net.pchinese.entitlement.domain.PlanCode;
import net.pchinese.entitlement.persistence.SubscriptionPlanEntity;
import net.pchinese.entitlement.persistence.SubscriptionPlanRepository;
import net.pchinese.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiMonitoringRuleService {
    private final AiMonitoringRuleRepository rules;
    private final SubscriptionPlanRepository plans;
    private final PlanPolicyVersionRepository policies;
    private final AiAdminAuditService auditService;

    public AiMonitoringRuleService(AiMonitoringRuleRepository rules, SubscriptionPlanRepository plans,
                                   PlanPolicyVersionRepository policies, AiAdminAuditService auditService) {
        this.rules = rules;
        this.plans = plans;
        this.policies = policies;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<MonitoringRuleView> list(UserPrincipal actor) {
        requireAdmin(actor);
        return rules.findAllByOrderByUpdatedAtDesc().stream().map(this::toView).toList();
    }

    @Transactional
    public MonitoringRuleView create(UserPrincipal actor, RuleCommand command) {
        requireAdmin(actor);
        Scope scope = resolveScope(command.planCode(), command.policyVersionId());
        Instant now = Instant.now();
        AiMonitoringRuleEntity rule = AiMonitoringRuleEntity.create(command.metric(), command.threshold(),
                command.evaluationWindow(), scope.plan(), scope.policy(), command.capability(), command.enabled(), actor.userId(), now);
        rules.save(rule);
        auditService.record(actor.userId(), AiAdminAuditEventType.MONITORING_RULE_CHANGED,
                AiAdminAuditTargetType.MONITORING_RULE, rule.getId(), "Monitoring rule created", null, safeSummary(rule), now);
        return toView(rule);
    }

    @Transactional
    public MonitoringRuleView update(UserPrincipal actor, UUID ruleId, UpdateRuleCommand command) {
        requireAdmin(actor);
        AiMonitoringRuleEntity rule = rules.findById(ruleId).orElseThrow(ApiException::notFound);
        if (rule.getVersion() != command.expectedVersion()) {
            throw ApiException.conflict("This monitoring rule has changed. Reload it before saving.");
        }
        Scope scope = resolveScope(command.planCode(), command.policyVersionId());
        Instant now = Instant.now();
        Map<String, Object> before = safeSummary(rule);
        rule.update(command.metric(), command.threshold(), command.evaluationWindow(), scope.plan(), scope.policy(),
                command.capability(), command.enabled(), actor.userId(), now);
        rules.save(rule);
        auditService.record(actor.userId(), AiAdminAuditEventType.MONITORING_RULE_CHANGED,
                AiAdminAuditTargetType.MONITORING_RULE, rule.getId(), "Monitoring rule updated", before, safeSummary(rule), now);
        return toView(rule);
    }

    private Scope resolveScope(PlanCode planCode, UUID policyVersionId) {
        SubscriptionPlanEntity plan = planCode == null ? null : plans.findByPlanCode(planCode).orElseThrow(ApiException::notFound);
        PlanPolicyVersionEntity policy = policyVersionId == null ? null : policies.findById(policyVersionId).orElseThrow(ApiException::notFound);
        if (plan != null && policy != null && !policy.getSubscriptionPlan().getSubscriptionPlanId().equals(plan.getSubscriptionPlanId())) {
            throw ApiException.validation("The selected policy version does not belong to the plan scope.");
        }
        return new Scope(plan, policy);
    }

    private MonitoringRuleView toView(AiMonitoringRuleEntity rule) {
        return new MonitoringRuleView(rule.getId(), rule.getMetric(), rule.getThreshold(), rule.getEvaluationWindow(),
                rule.getSubscriptionPlan() == null ? null : rule.getSubscriptionPlan().getPlanCode(),
                rule.getPolicyVersion() == null ? null : rule.getPolicyVersion().getId(), rule.getCapability(), rule.isEnabled(),
                rule.getVersion(), rule.getCreatedAt(), rule.getUpdatedAt());
    }

    private Map<String, Object> safeSummary(AiMonitoringRuleEntity rule) {
        return Map.of("metric", rule.getMetric().name(), "threshold", rule.getThreshold(),
                "window", rule.getEvaluationWindow().name(), "enabled", rule.isEnabled());
    }

    private void requireAdmin(UserPrincipal actor) {
        if (actor == null) throw ApiException.unauthenticated();
        if (!actor.isAdmin()) throw ApiException.forbidden();
    }

    private record Scope(SubscriptionPlanEntity plan, PlanPolicyVersionEntity policy) { }
    public record RuleCommand(MonitoringMetric metric, BigDecimal threshold, EvaluationWindow evaluationWindow,
                              PlanCode planCode, UUID policyVersionId, AiCapability capability, boolean enabled) { }
    public record UpdateRuleCommand(long expectedVersion, MonitoringMetric metric, BigDecimal threshold,
                                    EvaluationWindow evaluationWindow, PlanCode planCode, UUID policyVersionId,
                                    AiCapability capability, boolean enabled) { }
    public record MonitoringRuleView(UUID id, MonitoringMetric metric, BigDecimal threshold,
                                     EvaluationWindow evaluationWindow, PlanCode planCode, UUID policyVersionId,
                                     AiCapability capability, boolean enabled, long version,
                                     Instant createdAt, Instant updatedAt) { }
}
