package net.pchinese.allowance.application;

import net.pchinese.allowance.domain.AllowanceEventStatus;
import net.pchinese.allowance.domain.AllowanceFeatureType;
import net.pchinese.allowance.persistence.AiUsageEventEntity;
import net.pchinese.allowance.persistence.AiUsageEventRepository;
import net.pchinese.common.error.ApiException;
import net.pchinese.entitlement.application.EntitlementProvisioningService;
import net.pchinese.entitlement.persistence.UserEntitlementEntity;
import net.pchinese.entitlement.persistence.UserEntitlementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

@Service
public class AiAllowanceService {
    private static final int ROLLING_CYCLE_DAYS = 30;

    private final UserEntitlementRepository entitlementRepository;
    private final AiUsageEventRepository usageEventRepository;
    private final EntitlementProvisioningService provisioningService;

    public AiAllowanceService(UserEntitlementRepository entitlementRepository,
                              AiUsageEventRepository usageEventRepository,
                              EntitlementProvisioningService provisioningService) {
        this.entitlementRepository = entitlementRepository;
        this.usageEventRepository = usageEventRepository;
        this.provisioningService = provisioningService;
    }

    @Transactional
    public AiAllowanceCommands.AllowanceReservationResult reserveOrReuse(AiAllowanceCommands.ReserveAllowanceCommand command) {
        String fingerprintHash = computeFingerprintHash(command.featureType(), command.ownedOperationId());
        Instant now = Instant.now();

        var existingEventOpt = usageEventRepository.findByUserIdAndClientRequestId(command.userId(), command.clientRequestId());
        if (existingEventOpt.isPresent()) {
            AiUsageEventEntity existingEvent = existingEventOpt.get();
            if (!existingEvent.getRequestFingerprintHash().equalsIgnoreCase(fingerprintHash)) {
                throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT",
                        "Client request ID was used with a different operation fingerprint.");
            }
            UserEntitlementEntity entitlement = entitlementRepository.findActiveByUserIdLocked(command.userId())
                    .orElseGet(() -> provisioningService.ensureFreeEntitlement(command.userId()));
            entitlement.rollCycleIfExpired(now, ROLLING_CYCLE_DAYS);
            int remaining = Math.max(0, entitlement.getSubscriptionPlan().getAiQuotaUnits() - entitlement.getAiUsedUnits());
            return new AiAllowanceCommands.AllowanceReservationResult(
                    existingEvent.getAiUsageEventId(), existingEvent.getStatus(), true, remaining);
        }

        UserEntitlementEntity entitlement = entitlementRepository.findActiveByUserIdLocked(command.userId())
                .orElseGet(() -> {
                    provisioningService.ensureFreeEntitlement(command.userId());
                    return entitlementRepository.findActiveByUserIdLocked(command.userId())
                            .orElseThrow(() -> ApiException.notFound());
                });

        entitlement.rollCycleIfExpired(now, ROLLING_CYCLE_DAYS);

        int limit = entitlement.getSubscriptionPlan().getAiQuotaUnits();
        if (entitlement.getAiUsedUnits() + 1 > limit) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AI_QUOTA_EXCEEDED",
                    "AI quota limit reached for current cycle.");
        }

        entitlement.incrementUsedUnits(1, now);
        entitlementRepository.save(entitlement);

        AiUsageEventEntity newEvent = AiUsageEventEntity.createReserved(
                entitlement, command.userId(), command.clientRequestId(), fingerprintHash,
                command.featureType(), (short) 1, now);
        usageEventRepository.save(newEvent);

        int remaining = limit - entitlement.getAiUsedUnits();
        return new AiAllowanceCommands.AllowanceReservationResult(
                newEvent.getAiUsageEventId(), AllowanceEventStatus.RESERVED, false, remaining);
    }

    @Transactional
    public void succeed(UUID eventId, String providerReference) {
        AiUsageEventEntity event = usageEventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound());
        if (event.getStatus() == AllowanceEventStatus.RESERVED) {
            event.markSucceeded(providerReference, Instant.now());
            usageEventRepository.save(event);
        }
    }

    @Transactional
    public void refundOnce(UUID eventId, String failureCode) {
        AiUsageEventEntity event = usageEventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound());
        if (event.getStatus() == AllowanceEventStatus.RESERVED) {
            Instant now = Instant.now();
            UserEntitlementEntity entitlement = entitlementRepository.findActiveByUserIdLocked(event.getUserId())
                    .orElse(event.getUserEntitlement());
            entitlement.refundUsedUnits(1, now);
            entitlementRepository.save(entitlement);

            event.markFailedRefunded(failureCode, now);
            usageEventRepository.save(event);
        }
    }

    public static String computeFingerprintHash(AllowanceFeatureType featureType, String ownedOperationId) {
        String input = featureType.name() + ":" + ownedOperationId;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
