package net.pchinese.entitlement.application;

import net.pchinese.entitlement.domain.EntitlementStatus;
import net.pchinese.entitlement.domain.PlanCode;
import net.pchinese.entitlement.persistence.UserEntitlementEntity;
import net.pchinese.entitlement.persistence.UserEntitlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class EffectiveEntitlementService {

    private final UserEntitlementRepository entitlementRepository;
    private final EntitlementProvisioningService provisioningService;

    public EffectiveEntitlementService(
            UserEntitlementRepository entitlementRepository,
            EntitlementProvisioningService provisioningService
    ) {
        this.entitlementRepository = entitlementRepository;
        this.provisioningService = provisioningService;
    }

    @Transactional
    public PlanCode getEffectivePlanCodeLocked(UUID userId) {
        Instant now = Instant.now();
        Optional<UserEntitlementEntity> activeOpt = entitlementRepository.findActiveByUserIdLocked(userId);
        if (activeOpt.isEmpty()) {
            UserEntitlementEntity free = provisioningService.ensureFreeEntitlement(userId);
            return free.getSubscriptionPlan().getPlanCode();
        }

        UserEntitlementEntity active = activeOpt.get();
        if (active.isExpired(now)) {
            active.markExpired(now);
            entitlementRepository.save(active);
            UserEntitlementEntity free = provisioningService.ensureFreeEntitlement(userId);
            return free.getSubscriptionPlan().getPlanCode();
        }

        return active.getSubscriptionPlan().getPlanCode();
    }
}
