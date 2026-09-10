package net.pchinese.entitlement.application;

import net.pchinese.entitlement.persistence.UserEntitlementEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class EntitlementService {
    private static final int ROLLING_CYCLE_DAYS = 30;

    private final EntitlementProvisioningService provisioningService;

    public EntitlementService(EntitlementProvisioningService provisioningService) {
        this.provisioningService = provisioningService;
    }

    @Transactional
    public EntitlementSummary getSafeEntitlementSummary(UUID userId) {
        Instant now = Instant.now();
        UserEntitlementEntity entitlement = provisioningService.ensureFreeEntitlement(userId);
        
        entitlement.rollCycleIfExpired(now, ROLLING_CYCLE_DAYS);

        int limit = entitlement.getSubscriptionPlan().getAiQuotaUnits();
        int used = entitlement.getAiUsedUnits();
        int remaining = Math.max(0, limit - used);
        Instant cycleStart = entitlement.getAiQuotaPeriodStartedAt();
        Instant cycleEnd = cycleStart.plus(ROLLING_CYCLE_DAYS, ChronoUnit.DAYS);

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
