package net.pchinese.entitlement.application;

import net.pchinese.entitlement.domain.PlanCode;
import net.pchinese.entitlement.persistence.UserEntitlementEntity;
import net.pchinese.entitlement.persistence.UserEntitlementRepository;
import net.pchinese.vocabulary.application.VocabularyCapacityCommands;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class EntitlementLifecycleService {

    private final UserEntitlementRepository entitlementRepository;
    private final EntitlementProvisioningService provisioningService;
    private final VocabularyCapacityCommands capacityCommands;

    public EntitlementLifecycleService(
            UserEntitlementRepository entitlementRepository,
            EntitlementProvisioningService provisioningService,
            VocabularyCapacityCommands capacityCommands
    ) {
        this.entitlementRepository = entitlementRepository;
        this.provisioningService = provisioningService;
        this.capacityCommands = capacityCommands;
    }

    @Transactional
    public void reconcileEntitlementExpiry(UUID userId) {
        Instant now = Instant.now();
        Optional<UserEntitlementEntity> activeOpt = entitlementRepository.findActiveByUserIdLocked(userId);
        if (activeOpt.isEmpty()) {
            provisioningService.ensureFreeEntitlement(userId);
            return;
        }

        UserEntitlementEntity active = activeOpt.get();
        boolean wasPremium = active.getSubscriptionPlan().getPlanCode() == PlanCode.PREMIUM;
        if (active.isExpired(now)) {
            active.markExpired(now);
            entitlementRepository.save(active);
            provisioningService.ensureFreeEntitlement(userId);
            if (wasPremium) {
                capacityCommands.reconcilePremiumExpiryCapacity(userId);
            }
        }
    }
}
