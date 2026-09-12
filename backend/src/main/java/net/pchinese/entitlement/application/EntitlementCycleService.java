package net.pchinese.entitlement.application;

import net.pchinese.aiops.domain.PolicyStatus;
import net.pchinese.aiops.persistence.PlanPolicyVersionEntity;
import net.pchinese.aiops.persistence.PlanPolicyVersionRepository;
import net.pchinese.entitlement.domain.PlanCode;
import net.pchinese.entitlement.persistence.EntitlementAllowanceCycleEntity;
import net.pchinese.entitlement.persistence.EntitlementAllowanceCycleRepository;
import net.pchinese.entitlement.persistence.UserEntitlementEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class EntitlementCycleService {
    private final EntitlementAllowanceCycleRepository cycleRepository;
    private final PlanPolicyVersionRepository policyRepository;

    public EntitlementCycleService(EntitlementAllowanceCycleRepository cycleRepository,
                                   PlanPolicyVersionRepository policyRepository) {
        this.cycleRepository = cycleRepository;
        this.policyRepository = policyRepository;
    }

    /** Call while the entitlement row is held with a pessimistic write lock. */
    public EntitlementAllowanceCycleEntity ensureCurrentCycleLocked(UserEntitlementEntity entitlement, Instant now) {
        EntitlementAllowanceCycleEntity cycle = cycleRepository.findCurrentByEntitlementIdLocked(entitlement.getUserEntitlementId())
                .orElseGet(() -> createCurrent(entitlement, entitlement.getCurrentPolicyVersion(), now));
        if (now.isBefore(cycle.getCycleEndsAt())) {
            return cycle;
        }
        cycle.close(now);
        cycleRepository.save(cycle);
        PlanPolicyVersionEntity policy = policyRepository.findCurrentByPlanCodeLocked(PlanCode.FREE)
                .filter(current -> current.getStatus() == PolicyStatus.PUBLISHED)
                .orElseThrow(() -> new IllegalStateException("A current Free policy is required."));
        entitlement.synchronizeCycleSnapshot(policy, now, 0, now);
        return createCurrent(entitlement, policy, now);
    }

    private EntitlementAllowanceCycleEntity createCurrent(UserEntitlementEntity entitlement,
                                                           PlanPolicyVersionEntity policy,
                                                           Instant now) {
        Instant endsAt = policy.getAllowancePeriod().name().equals("DAY")
                ? now.plus(1, ChronoUnit.DAYS)
                : now.plus(1, ChronoUnit.MONTHS);
        EntitlementAllowanceCycleEntity cycle = EntitlementAllowanceCycleEntity.createCurrent(entitlement, policy, now, endsAt);
        return cycleRepository.save(cycle);
    }
}
