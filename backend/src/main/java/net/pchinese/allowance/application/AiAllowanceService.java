package net.pchinese.allowance.application;

import net.pchinese.allowance.domain.AllowanceEventStatus;
import net.pchinese.allowance.domain.AllowanceFeatureType;
import net.pchinese.allowance.persistence.AiUsageEventEntity;
import net.pchinese.allowance.persistence.AiUsageEventRepository;
import net.pchinese.aiops.application.AiOperationalMeasurementService;
import net.pchinese.aiops.domain.AiCapability;
import net.pchinese.aiops.domain.ProviderTelemetry;
import net.pchinese.common.error.ApiException;
import net.pchinese.entitlement.application.EntitlementCycleService;
import net.pchinese.entitlement.application.EntitlementProvisioningService;
import net.pchinese.entitlement.persistence.EntitlementAllowanceCycleEntity;
import net.pchinese.entitlement.persistence.UserEntitlementEntity;
import net.pchinese.entitlement.persistence.UserEntitlementRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final int LEGACY_ROLLING_CYCLE_DAYS = 30;
    private final UserEntitlementRepository entitlementRepository;
    private final AiUsageEventRepository usageEventRepository;
    private final EntitlementProvisioningService provisioningService;
    private final EntitlementCycleService cycleService;
    private final AiOperationalMeasurementService measurementService;

    @Autowired
    public AiAllowanceService(UserEntitlementRepository entitlementRepository,
                              AiUsageEventRepository usageEventRepository,
                              EntitlementProvisioningService provisioningService,
                              EntitlementCycleService cycleService,
                              AiOperationalMeasurementService measurementService) {
        this.entitlementRepository = entitlementRepository;
        this.usageEventRepository = usageEventRepository;
        this.provisioningService = provisioningService;
        this.cycleService = cycleService;
        this.measurementService = measurementService;
    }

    /** Kept solely for the pre-F12 unit fixtures; runtime wiring always uses the full constructor. */
    public AiAllowanceService(UserEntitlementRepository entitlementRepository,
                              AiUsageEventRepository usageEventRepository,
                              EntitlementProvisioningService provisioningService) {
        this.entitlementRepository = entitlementRepository;
        this.usageEventRepository = usageEventRepository;
        this.provisioningService = provisioningService;
        this.cycleService = null;
        this.measurementService = null;
    }

    @Transactional
    public AiAllowanceCommands.AllowanceReservationResult reserveOrReuse(AiAllowanceCommands.ReserveAllowanceCommand command) {
        if (cycleService == null) return reserveOrReuseLegacy(command);
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
            EntitlementAllowanceCycleEntity cycle = cycleService.ensureCurrentCycleLocked(entitlement, now);
            int remaining = Math.max(0, cycle.getAllowanceLimit() - cycle.getUsedUnits());
            measurementService.createOrReusePending(existingEvent, now);
            return new AiAllowanceCommands.AllowanceReservationResult(
                    existingEvent.getAiUsageEventId(), existingEvent.getStatus(), true, remaining);
        }

        UserEntitlementEntity entitlement = entitlementRepository.findActiveByUserIdLocked(command.userId())
                .orElseGet(() -> {
                    provisioningService.ensureFreeEntitlement(command.userId());
                    return entitlementRepository.findActiveByUserIdLocked(command.userId())
                            .orElseThrow(() -> ApiException.notFound());
                });

        // The entitlement lock serializes competing first attempts. Re-read the idempotency key
        // after acquiring it so concurrent retries cannot reserve two units.
        var concurrentEvent = usageEventRepository.findByUserIdAndClientRequestId(command.userId(), command.clientRequestId());
        if (concurrentEvent.isPresent()) {
            AiUsageEventEntity event = concurrentEvent.get();
            if (!event.getRequestFingerprintHash().equalsIgnoreCase(fingerprintHash)) {
                throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT",
                        "Client request ID was used with a different operation fingerprint.");
            }
            EntitlementAllowanceCycleEntity currentCycle = cycleService.ensureCurrentCycleLocked(entitlement, now);
            measurementService.createOrReusePending(event, now);
            return new AiAllowanceCommands.AllowanceReservationResult(event.getAiUsageEventId(), event.getStatus(), true,
                    Math.max(0, currentCycle.getAllowanceLimit() - currentCycle.getUsedUnits()));
        }

        EntitlementAllowanceCycleEntity cycle = cycleService.ensureCurrentCycleLocked(entitlement, now);
        int limit = cycle.getAllowanceLimit();
        if (cycle.getUsedUnits() + 1 > limit) {
            measurementService.createOrReuseQuotaDenied(command.userId(), command.clientRequestId(),
                    cycle.getPolicyVersion().getId(), AiCapability.valueOf(command.featureType().name()), now);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AI_QUOTA_EXCEEDED",
                    "AI quota limit reached for current cycle.");
        }

        cycle.incrementUsedUnits(1);
        entitlement.synchronizeCycleSnapshot(cycle.getPolicyVersion(), cycle.getCycleStartedAt(), cycle.getUsedUnits(), now);
        entitlementRepository.save(entitlement);

        AiUsageEventEntity newEvent = AiUsageEventEntity.createReserved(
                entitlement, cycle, command.userId(), command.clientRequestId(), fingerprintHash,
                command.featureType(), (short) 1, now);
        usageEventRepository.save(newEvent);
        measurementService.createOrReusePending(newEvent, now);

        int remaining = limit - cycle.getUsedUnits();
        return new AiAllowanceCommands.AllowanceReservationResult(
                newEvent.getAiUsageEventId(), AllowanceEventStatus.RESERVED, false, remaining);
    }

    @Transactional
    public void succeed(UUID eventId, String providerReference) {
        succeed(eventId, providerReference, ProviderTelemetry.unavailable(), 0);
    }

    @Transactional
    public void succeed(UUID eventId, String providerReference, ProviderTelemetry telemetry, long endToEndDurationMs) {
        AiUsageEventEntity event = (cycleService == null ? usageEventRepository.findById(eventId) : usageEventRepository.findByIdLocked(eventId))
                .orElseThrow(() -> ApiException.notFound());
        if (event.getStatus() == AllowanceEventStatus.RESERVED) {
            Instant now = Instant.now();
            event.markSucceeded(providerReference, now);
            usageEventRepository.save(event);
            if (measurementService != null) measurementService.finalizeSuccess(event, telemetry, endToEndDurationMs, now);
        }
    }

    @Transactional
    public void refundOnce(UUID eventId, String failureCode) {
        refundOnce(eventId, failureCode, ProviderTelemetry.unavailable(), 0);
    }

    @Transactional
    public void refundOnce(UUID eventId, String failureCode, ProviderTelemetry telemetry, long endToEndDurationMs) {
        AiUsageEventEntity event = (cycleService == null ? usageEventRepository.findById(eventId) : usageEventRepository.findByIdLocked(eventId))
                .orElseThrow(() -> ApiException.notFound());
        if (event.getStatus() == AllowanceEventStatus.RESERVED) {
            Instant now = Instant.now();
            UserEntitlementEntity entitlement = entitlementRepository.findActiveByUserIdLocked(event.getUserId())
                    .orElse(event.getUserEntitlement());
            if (cycleService == null) {
                entitlement.refundUsedUnits(1, now);
                entitlementRepository.save(entitlement);
                event.markFailedRefunded(failureCode, now);
                usageEventRepository.save(event);
                return;
            }
            EntitlementAllowanceCycleEntity cycle = event.getAllowanceCycle();
            cycle.refundUsedUnits(event.getRequestedUnits());
            entitlement.synchronizeCycleSnapshot(cycle.getPolicyVersion(), cycle.getCycleStartedAt(), cycle.getUsedUnits(), now);
            entitlementRepository.save(entitlement);

            event.markFailedRefunded(failureCode, now);
            usageEventRepository.save(event);
            if (measurementService != null) measurementService.finalizeRefund(event, telemetry, endToEndDurationMs, now);
        }
    }

    private AiAllowanceCommands.AllowanceReservationResult reserveOrReuseLegacy(AiAllowanceCommands.ReserveAllowanceCommand command) {
        String fingerprintHash = computeFingerprintHash(command.featureType(), command.ownedOperationId());
        Instant now = Instant.now();
        var existing = usageEventRepository.findByUserIdAndClientRequestId(command.userId(), command.clientRequestId());
        if (existing.isPresent()) {
            AiUsageEventEntity event = existing.get();
            if (!event.getRequestFingerprintHash().equalsIgnoreCase(fingerprintHash)) {
                throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", "Client request ID was used with a different operation fingerprint.");
            }
            UserEntitlementEntity entitlement = entitlementRepository.findActiveByUserIdLocked(command.userId())
                    .orElseGet(() -> provisioningService.ensureFreeEntitlement(command.userId()));
            entitlement.rollCycleIfExpired(now, LEGACY_ROLLING_CYCLE_DAYS);
            int remaining = Math.max(0, entitlement.getSubscriptionPlan().getAiQuotaUnits() - entitlement.getAiUsedUnits());
            return new AiAllowanceCommands.AllowanceReservationResult(event.getAiUsageEventId(), event.getStatus(), true, remaining);
        }
        UserEntitlementEntity entitlement = entitlementRepository.findActiveByUserIdLocked(command.userId())
                .orElseGet(() -> provisioningService.ensureFreeEntitlement(command.userId()));
        entitlement.rollCycleIfExpired(now, LEGACY_ROLLING_CYCLE_DAYS);
        int limit = entitlement.getSubscriptionPlan().getAiQuotaUnits();
        if (entitlement.getAiUsedUnits() + 1 > limit) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AI_QUOTA_EXCEEDED", "AI quota limit reached for current cycle.");
        }
        entitlement.incrementUsedUnits(1, now);
        entitlementRepository.save(entitlement);
        AiUsageEventEntity event = AiUsageEventEntity.createReserved(entitlement, command.userId(), command.clientRequestId(),
                fingerprintHash, command.featureType(), (short) 1, now);
        usageEventRepository.save(event);
        return new AiAllowanceCommands.AllowanceReservationResult(event.getAiUsageEventId(), AllowanceEventStatus.RESERVED,
                false, limit - entitlement.getAiUsedUnits());
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
