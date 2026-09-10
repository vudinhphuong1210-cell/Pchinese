package net.pchinese.allowance;

import net.pchinese.allowance.application.AiAllowanceCommands;
import net.pchinese.allowance.application.AiAllowanceService;
import net.pchinese.allowance.domain.AllowanceEventStatus;
import net.pchinese.allowance.domain.AllowanceFeatureType;
import net.pchinese.allowance.persistence.AiUsageEventEntity;
import net.pchinese.allowance.persistence.AiUsageEventRepository;
import net.pchinese.common.error.ApiException;
import net.pchinese.entitlement.application.EntitlementProvisioningService;
import net.pchinese.entitlement.domain.EntitlementSourceType;
import net.pchinese.entitlement.domain.EntitlementStatus;
import net.pchinese.entitlement.domain.PlanCode;
import net.pchinese.entitlement.domain.PlanStatus;
import net.pchinese.entitlement.domain.QuotaPeriod;
import net.pchinese.entitlement.persistence.SubscriptionPlanEntity;
import net.pchinese.entitlement.persistence.UserEntitlementEntity;
import net.pchinese.entitlement.persistence.UserEntitlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiAllowanceServiceTest {
    private UserEntitlementRepository entitlementRepository;
    private AiUsageEventRepository usageEventRepository;
    private EntitlementProvisioningService provisioningService;
    private AiAllowanceService service;

    private UUID userId;
    private SubscriptionPlanEntity freePlan;
    private UserEntitlementEntity activeEntitlement;

    @BeforeEach
    void setUp() {
        entitlementRepository = mock(UserEntitlementRepository.class);
        usageEventRepository = mock(AiUsageEventRepository.class);
        provisioningService = mock(EntitlementProvisioningService.class);
        service = new AiAllowanceService(entitlementRepository, usageEventRepository, provisioningService);

        userId = UUID.randomUUID();
        Instant now = Instant.now();
        freePlan = SubscriptionPlanEntity.create(PlanCode.FREE, "Free", (short) 2, 30, QuotaPeriod.MONTH, PlanStatus.ACTIVE, now);
        activeEntitlement = UserEntitlementEntity.createDefaultFree(userId, freePlan, now);

        when(entitlementRepository.findActiveByUserIdLocked(userId)).thenReturn(Optional.of(activeEntitlement));
        when(provisioningService.ensureFreeEntitlement(userId)).thenReturn(activeEntitlement);
    }

    @Test
    void reserveOrReuse_whenQuotaAvailable_reservesAndIncrementsUnits() {
        UUID reqId = UUID.randomUUID();
        AiAllowanceCommands.ReserveAllowanceCommand cmd = new AiAllowanceCommands.ReserveAllowanceCommand(
                userId, AllowanceFeatureType.AI_BUDDY, "op-1", reqId);

        when(usageEventRepository.findByUserIdAndClientRequestId(userId, reqId)).thenReturn(Optional.empty());

        AiAllowanceCommands.AllowanceReservationResult result = service.reserveOrReuse(cmd);

        assertEquals(AllowanceEventStatus.RESERVED, result.status());
        assertFalse(result.reused());
        assertEquals(29, result.remainingUnits());
        assertEquals(1, activeEntitlement.getAiUsedUnits());
        verify(usageEventRepository).save(any(AiUsageEventEntity.class));
    }

    @Test
    void reserveOrReuse_whenQuotaExceeded_throws429() {
        activeEntitlement.incrementUsedUnits(30, Instant.now());
        UUID reqId = UUID.randomUUID();
        AiAllowanceCommands.ReserveAllowanceCommand cmd = new AiAllowanceCommands.ReserveAllowanceCommand(
                userId, AllowanceFeatureType.AI_BUDDY, "op-1", reqId);

        when(usageEventRepository.findByUserIdAndClientRequestId(userId, reqId)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.reserveOrReuse(cmd));
        assertEquals("AI_QUOTA_EXCEEDED", ex.code());
    }

    @Test
    void reserveOrReuse_whenSameClientRequestIdAndSameFingerprint_reusesExisting() {
        UUID reqId = UUID.randomUUID();
        String fingerprint = AiAllowanceService.computeFingerprintHash(AllowanceFeatureType.AI_BUDDY, "op-1");
        Instant now = Instant.now();
        AiUsageEventEntity existingEvent = AiUsageEventEntity.createReserved(
                activeEntitlement, userId, reqId, fingerprint, AllowanceFeatureType.AI_BUDDY, (short) 1, now);

        when(usageEventRepository.findByUserIdAndClientRequestId(userId, reqId)).thenReturn(Optional.of(existingEvent));

        AiAllowanceCommands.ReserveAllowanceCommand cmd = new AiAllowanceCommands.ReserveAllowanceCommand(
                userId, AllowanceFeatureType.AI_BUDDY, "op-1", reqId);

        AiAllowanceCommands.AllowanceReservationResult result = service.reserveOrReuse(cmd);

        assertTrue(result.reused());
        assertEquals(existingEvent.getAiUsageEventId(), result.eventId());
        assertEquals(AllowanceEventStatus.RESERVED, result.status());
    }

    @Test
    void reserveOrReuse_whenSameClientRequestIdAndDifferentFingerprint_throws409() {
        UUID reqId = UUID.randomUUID();
        String oldFingerprint = AiAllowanceService.computeFingerprintHash(AllowanceFeatureType.AI_BUDDY, "op-1");
        Instant now = Instant.now();
        AiUsageEventEntity existingEvent = AiUsageEventEntity.createReserved(
                activeEntitlement, userId, reqId, oldFingerprint, AllowanceFeatureType.AI_BUDDY, (short) 1, now);

        when(usageEventRepository.findByUserIdAndClientRequestId(userId, reqId)).thenReturn(Optional.of(existingEvent));

        AiAllowanceCommands.ReserveAllowanceCommand cmd = new AiAllowanceCommands.ReserveAllowanceCommand(
                userId, AllowanceFeatureType.AI_BUDDY, "op-DIFFERENT", reqId);

        ApiException ex = assertThrows(ApiException.class, () -> service.reserveOrReuse(cmd));
        assertEquals("IDEMPOTENCY_CONFLICT", ex.code());
    }

    @Test
    void succeed_whenReserved_marksSucceeded() {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();
        AiUsageEventEntity event = AiUsageEventEntity.createReserved(
                activeEntitlement, userId, UUID.randomUUID(), "hash", AllowanceFeatureType.AI_BUDDY, (short) 1, now);

        when(usageEventRepository.findById(eventId)).thenReturn(Optional.of(event));

        service.succeed(eventId, "ref-123");

        assertEquals(AllowanceEventStatus.SUCCEEDED, event.getStatus());
        assertEquals("ref-123", event.getAiServiceRequestReference());
    }

    @Test
    void refundOnce_whenReserved_refundsQuotaAndMarksFailedRefunded() {
        activeEntitlement.incrementUsedUnits(1, Instant.now());
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();
        AiUsageEventEntity event = AiUsageEventEntity.createReserved(
                activeEntitlement, userId, UUID.randomUUID(), "hash", AllowanceFeatureType.AI_BUDDY, (short) 1, now);

        when(usageEventRepository.findById(eventId)).thenReturn(Optional.of(event));

        service.refundOnce(eventId, "TIMEOUT");

        assertEquals(0, activeEntitlement.getAiUsedUnits());
        assertEquals(AllowanceEventStatus.FAILED_REFUNDED, event.getStatus());
        assertEquals("TIMEOUT", event.getFailureCode());
    }

    @Test
    void refundOnce_whenAlreadyRefunded_isIdempotent() {
        UUID eventId = UUID.randomUUID();
        Instant now = Instant.now();
        AiUsageEventEntity event = AiUsageEventEntity.createReserved(
                activeEntitlement, userId, UUID.randomUUID(), "hash", AllowanceFeatureType.AI_BUDDY, (short) 1, now);
        event.markFailedRefunded("TIMEOUT", now);

        when(usageEventRepository.findById(eventId)).thenReturn(Optional.of(event));

        service.refundOnce(eventId, "TIMEOUT");

        verify(entitlementRepository, never()).findActiveByUserIdLocked(any());
    }

    @Test
    void rollover_whenCycleExpired_resetsUnitsAndUpdatesPeriodStart() {
        Instant thirtyOneDaysAgo = Instant.now().minus(31, ChronoUnit.DAYS);
        UserEntitlementEntity oldEntitlement = UserEntitlementEntity.createDefaultFree(userId, freePlan, thirtyOneDaysAgo);
        oldEntitlement.incrementUsedUnits(20, thirtyOneDaysAgo);

        boolean rolled = oldEntitlement.rollCycleIfExpired(Instant.now(), 30);

        assertTrue(rolled);
        assertEquals(0, oldEntitlement.getAiUsedUnits());
    }
}
