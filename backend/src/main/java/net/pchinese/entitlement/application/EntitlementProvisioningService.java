package net.pchinese.entitlement.application;

import net.pchinese.entitlement.domain.EntitlementStatus;
import net.pchinese.entitlement.domain.PlanCode;
import net.pchinese.entitlement.domain.PlanStatus;
import net.pchinese.entitlement.domain.QuotaPeriod;
import net.pchinese.entitlement.persistence.SubscriptionPlanEntity;
import net.pchinese.entitlement.persistence.SubscriptionPlanRepository;
import net.pchinese.entitlement.persistence.UserEntitlementEntity;
import net.pchinese.entitlement.persistence.UserEntitlementRepository;
import net.pchinese.aiops.domain.PolicyStatus;
import net.pchinese.aiops.persistence.PlanPolicyVersionEntity;
import net.pchinese.aiops.persistence.PlanPolicyVersionRepository;
import net.pchinese.common.error.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class EntitlementProvisioningService {
    private final SubscriptionPlanRepository planRepository;
    private final UserEntitlementRepository entitlementRepository;
    private final PlanPolicyVersionRepository policyRepository;

    public EntitlementProvisioningService(SubscriptionPlanRepository planRepository,
                                         UserEntitlementRepository entitlementRepository,
                                         PlanPolicyVersionRepository policyRepository) {
        this.planRepository = planRepository;
        this.entitlementRepository = entitlementRepository;
        this.policyRepository = policyRepository;
    }

    @Transactional
    public UserEntitlementEntity ensureFreeEntitlement(UUID userId) {
        Instant now = Instant.now();
        return entitlementRepository.findByUserIdAndStatus(userId, EntitlementStatus.ACTIVE)
                .orElseGet(() -> {
                    SubscriptionPlanEntity freePlan = planRepository.findByPlanCode(PlanCode.FREE)
                            .orElseGet(() -> planRepository.save(SubscriptionPlanEntity.create(
                                    PlanCode.FREE, "Free", (short) 2, 30, QuotaPeriod.MONTH, PlanStatus.ACTIVE, now)));
                    PlanPolicyVersionEntity policy = policyRepository
                            .findCurrentByPlanCodeLocked(PlanCode.FREE)
                            .orElseThrow(() -> ApiException.conflict("No current Free policy is available."));
                    UserEntitlementEntity newEntitlement = UserEntitlementEntity.createDefaultFree(userId, freePlan, policy, now);
                    return entitlementRepository.save(newEntitlement);
                });
    }
}
