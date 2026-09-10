package net.pchinese.allowance;

import net.pchinese.allowance.application.AiAllowanceCommands;
import net.pchinese.allowance.application.AiAllowanceService;
import net.pchinese.allowance.domain.AllowanceEventStatus;
import net.pchinese.allowance.domain.AllowanceFeatureType;
import net.pchinese.common.error.ApiException;
import net.pchinese.entitlement.application.EntitlementProvisioningService;
import net.pchinese.entitlement.persistence.UserEntitlementEntity;
import net.pchinese.users.persistence.UserEntity;
import net.pchinese.users.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
class AiAllowanceServiceIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired UserRepository users;
    @Autowired EntitlementProvisioningService provisioningService;
    @Autowired AiAllowanceService allowanceService;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private UserEntity user;
    private UserEntitlementEntity entitlement;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        String lookupHash = ("allowance" + UUID.randomUUID()).replace("-", "");
        user = users.save(UserEntity.pending(new byte[] {1}, (lookupHash + "0".repeat(64)).substring(0, 64), "hash", now));
        user.activate(now);
        user = users.save(user);
        entitlement = provisioningService.ensureFreeEntitlement(user.getUserId());
    }

    @Test
    void concurrentReservations_atBoundary_atMostAllowedCountSucceeds() throws Exception {
        int threads = 10;
        int reservationsPerThread = 4; // Total 40 requests against 30-unit quota
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger quotaExceededCount = new AtomicInteger(0);

        for (int i = 0; i < threads * reservationsPerThread; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    AiAllowanceCommands.ReserveAllowanceCommand cmd = new AiAllowanceCommands.ReserveAllowanceCommand(
                            user.getUserId(), AllowanceFeatureType.AI_BUDDY, "op-concurrent", UUID.randomUUID());
                    allowanceService.reserveOrReuse(cmd);
                    successCount.incrementAndGet();
                } catch (ApiException ex) {
                    if ("AI_QUOTA_EXCEEDED".equals(ex.code())) {
                        quotaExceededCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        assertEquals(30, successCount.get(), "Exactly 30 reservations must succeed");
        assertEquals(10, quotaExceededCount.get(), "10 reservations must fail with AI_QUOTA_EXCEEDED");
    }

    @Test
    void reusedFingerprint_isIdempotent() {
        UUID reqId = UUID.randomUUID();
        AiAllowanceCommands.ReserveAllowanceCommand cmd1 = new AiAllowanceCommands.ReserveAllowanceCommand(
                user.getUserId(), AllowanceFeatureType.SHADOWING_ASSESSMENT, "op-1", reqId);

        AiAllowanceCommands.AllowanceReservationResult res1 = allowanceService.reserveOrReuse(cmd1);
        assertEquals(AllowanceEventStatus.RESERVED, res1.status());
        assertEquals(false, res1.reused());

        AiAllowanceCommands.AllowanceReservationResult res2 = allowanceService.reserveOrReuse(cmd1);
        assertEquals(res1.eventId(), res2.eventId());
        assertEquals(true, res2.reused());
    }

    @Test
    void changedFingerprint_throwsConflict() {
        UUID reqId = UUID.randomUUID();
        AiAllowanceCommands.ReserveAllowanceCommand cmd1 = new AiAllowanceCommands.ReserveAllowanceCommand(
                user.getUserId(), AllowanceFeatureType.SHADOWING_ASSESSMENT, "op-1", reqId);
        allowanceService.reserveOrReuse(cmd1);

        AiAllowanceCommands.ReserveAllowanceCommand cmd2 = new AiAllowanceCommands.ReserveAllowanceCommand(
                user.getUserId(), AllowanceFeatureType.SHADOWING_ASSESSMENT, "op-2-DIFFERENT", reqId);

        ApiException ex = assertThrows(ApiException.class, () -> allowanceService.reserveOrReuse(cmd2));
        assertEquals("IDEMPOTENCY_CONFLICT", ex.code());
    }

    @Test
    void refundOnce_onlyRefundsSingleTime() {
        UUID reqId = UUID.randomUUID();
        AiAllowanceCommands.ReserveAllowanceCommand cmd = new AiAllowanceCommands.ReserveAllowanceCommand(
                user.getUserId(), AllowanceFeatureType.AI_BUDDY, "op-refund", reqId);

        AiAllowanceCommands.AllowanceReservationResult res = allowanceService.reserveOrReuse(cmd);

        allowanceService.refundOnce(res.eventId(), "TIMEOUT");
        allowanceService.refundOnce(res.eventId(), "TIMEOUT"); // Second call

        // Verify remaining units after refunding once is back to 30
        AiAllowanceCommands.ReserveAllowanceCommand checkCmd = new AiAllowanceCommands.ReserveAllowanceCommand(
                user.getUserId(), AllowanceFeatureType.AI_BUDDY, "op-check", UUID.randomUUID());
        AiAllowanceCommands.AllowanceReservationResult checkRes = allowanceService.reserveOrReuse(checkCmd);
        assertEquals(29, checkRes.remainingUnits());
    }
}
