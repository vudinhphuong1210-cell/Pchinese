package net.pchinese.aiops.application;

import net.pchinese.aiops.domain.AllowancePeriod;
import net.pchinese.aiops.domain.AvailabilityState;
import net.pchinese.aiops.domain.AiAdminAuditEventType;
import net.pchinese.aiops.domain.AiAdminAuditTargetType;
import net.pchinese.aiops.domain.PolicyStatus;
import net.pchinese.aiops.domain.PriceInterval;
import net.pchinese.aiops.persistence.PlanPolicyVersionEntity;
import net.pchinese.aiops.persistence.PlanPolicyVersionRepository;
import net.pchinese.common.error.ApiException;
import net.pchinese.entitlement.domain.PlanCode;
import net.pchinese.entitlement.persistence.SubscriptionPlanEntity;
import net.pchinese.entitlement.persistence.SubscriptionPlanRepository;
import net.pchinese.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AiPlanPolicyService {
    private final SubscriptionPlanRepository plans;
    private final PlanPolicyVersionRepository policies;
    private final AiAdminAuditService auditService;

    public AiPlanPolicyService(SubscriptionPlanRepository plans, PlanPolicyVersionRepository policies,
                               AiAdminAuditService auditService) {
        this.plans = plans;
        this.policies = policies;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PolicyView> listCurrent(UserPrincipal actor) {
        requireAdmin(actor);
        return List.of(PlanCode.FREE, PlanCode.PREMIUM).stream()
                .map(code -> policies.findBySubscriptionPlan_PlanCodeAndStatus(code, PolicyStatus.PUBLISHED)
                        .map(this::toView).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PolicyView> listHistory(UserPrincipal actor, PlanCode code, Pageable pageable) {
        requireAdmin(actor);
        return policies.findBySubscriptionPlan_PlanCodeOrderByRevisionNumberDesc(code, pageable).map(this::toView);
    }

    @Transactional
    public PolicyView publish(UserPrincipal actor, PlanCode code, PublishPolicyCommand command) {
        requireAdmin(actor);
        SubscriptionPlanEntity plan = plans.findByPlanCode(code).orElseThrow(ApiException::notFound);
        PlanPolicyVersionEntity current = policies.findCurrentByPlanCodeLocked(code).orElseThrow(ApiException::notFound);
        if (current.getVersion() != command.expectedCurrentVersion()) {
            throw ApiException.conflict("This policy has changed. Reload the latest policy before publishing.");
        }
        Instant now = Instant.now();
        Map<String, Object> before = safeSummary(current);
        current.supersede();
        policies.saveAndFlush(current);
        PlanPolicyVersionEntity next = PlanPolicyVersionEntity.publish(plan, current.getRevisionNumber() + 1,
                actor.userId(), command.reason().trim(), command.displayName().trim(), blankToNull(command.description()),
                command.benefits().stream().map(String::trim).toList(), command.availabilityState(),
                command.priceAmount(), command.currency().toUpperCase(Locale.ROOT), command.priceInterval(),
                blankToNull(command.displayLabel()), command.allowanceUnits(), command.allowancePeriod(), now);
        policies.save(next);
        auditService.record(actor.userId(), AiAdminAuditEventType.POLICY_PUBLISHED, AiAdminAuditTargetType.PLAN_POLICY,
                next.getId(), command.reason().trim(), before, safeSummary(next), now);
        return toView(next);
    }

    @Transactional
    public PolicyView retire(UserPrincipal actor, PlanCode code, RetirePlanCommand command) {
        requireAdmin(actor);
        PlanPolicyVersionEntity current = policies.findCurrentByPlanCodeLocked(code).orElseThrow(ApiException::notFound);
        if (current.getVersion() != command.expectedCurrentVersion()) {
            throw ApiException.conflict("This policy has changed. Reload the latest policy before retiring it.");
        }
        if (code == PlanCode.FREE) {
            throw ApiException.conflict("The current Free policy cannot be retired because eligible learners require one.");
        }
        Instant now = Instant.now();
        Map<String, Object> before = safeSummary(current);
        current.retire(now);
        policies.save(current);
        auditService.record(actor.userId(), AiAdminAuditEventType.PLAN_RETIRED, AiAdminAuditTargetType.PLAN_POLICY,
                current.getId(), command.reason().trim(), before, safeSummary(current), now);
        return toView(current);
    }

    private void requireAdmin(UserPrincipal actor) {
        if (actor == null) throw ApiException.unauthenticated();
        if (!actor.isAdmin()) throw ApiException.forbidden();
    }

    private PolicyView toView(PlanPolicyVersionEntity policy) {
        return new PolicyView(policy.getId(), policy.getSubscriptionPlan().getPlanCode(), policy.getRevisionNumber(),
                policy.getStatus(), policy.getDisplayName(), policy.getDescription(), policy.getBenefits(),
                policy.getAvailabilityState(), policy.getPriceAmount(), policy.getCurrency(), policy.getPriceInterval(),
                policy.getDisplayLabel(), policy.getAllowanceUnits(), policy.getAllowancePeriod(), policy.getVersion(),
                policy.getCreatedAt(), policy.getRetiredAt());
    }

    private Map<String, Object> safeSummary(PlanPolicyVersionEntity policy) {
        return Map.of("revisionNumber", policy.getRevisionNumber(), "status", policy.getStatus().name(),
                "allowanceUnits", policy.getAllowanceUnits(), "allowancePeriod", policy.getAllowancePeriod().name(),
                "availabilityState", policy.getAvailabilityState().name());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record PublishPolicyCommand(long expectedCurrentVersion, String reason, String displayName,
                                       String description, List<String> benefits, AvailabilityState availabilityState,
                                       BigDecimal priceAmount, String currency, PriceInterval priceInterval,
                                       String displayLabel, int allowanceUnits, AllowancePeriod allowancePeriod) { }
    public record RetirePlanCommand(long expectedCurrentVersion, String reason) { }
    public record PolicyView(java.util.UUID id, PlanCode planCode, int revisionNumber, PolicyStatus status,
                             String displayName, String description, List<String> benefits,
                             AvailabilityState availabilityState, BigDecimal priceAmount, String currency,
                             PriceInterval priceInterval, String displayLabel, int allowanceUnits,
                             AllowancePeriod allowancePeriod, long version, Instant createdAt, Instant retiredAt) { }
}
