package net.pchinese.entitlement.application;

import net.pchinese.entitlement.persistence.UserEntitlementEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class EntitlementService {
    private final EntitlementProvisioningService provisioningService;
    private final EntitlementCycleService cycleService;

    public EntitlementService(EntitlementProvisioningService provisioningService, EntitlementCycleService cycleService) {
        this.provisioningService = provisioningService;
        this.cycleService = cycleService;
    }

    @Transactional
    public EntitlementSummary getSafeEntitlementSummary(UUID userId) {
        Instant now = Instant.now();
        UserEntitlementEntity entitlement = provisioningService.ensureFreeEntitlement(userId);
        
        var cycle = cycleService.ensureCurrentCycleLocked(entitlement, now);
        int limit = cycle.getAllowanceLimit();
        int used = cycle.getUsedUnits();
        int remaining = Math.max(0, limit - used);
        Instant cycleStart = cycle.getCycleStartedAt();
        Instant cycleEnd = cycle.getCycleEndsAt();

        return new EntitlementSummary(
                entitlement.getSubscriptionPlan().getPlanCode().name(),
                limit,
                used,
                remaining,
                cycleStart,
                cycleEnd
        );
    }

    public record EntitlementSummary(
            String planCode,
            int allowanceLimit,
            int usedUnits,
            int remainingUnits,
            Instant cycleStartAt,
            Instant cycleEndAt
    ) { }
}
