package net.pchinese.profile;

import net.pchinese.auth.domain.Platform;
import net.pchinese.auth.persistence.AuthSessionEntity;
import net.pchinese.auth.persistence.AuthSessionRepository;
import net.pchinese.entitlement.application.EntitlementProvisioningService;
import net.pchinese.entitlement.persistence.UserEntitlementEntity;
import net.pchinese.security.JwtService;
import net.pchinese.users.persistence.UserEntity;
import net.pchinese.users.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CurrentUserEntitlementIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired AuthSessionRepository sessions;
    @Autowired JwtService jwtService;
    @Autowired EntitlementProvisioningService provisioningService;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void activeLearnerReceivesFreeEntitlementAndSafeSummary() throws Exception {
        Instant now = Instant.now();
        String lookupHash = ("entitle" + UUID.randomUUID()).replace("-", "");
        UserEntity user = users.save(UserEntity.pending(new byte[] {1}, (lookupHash + "0".repeat(64)).substring(0, 64), "hash", now));
        user.activate(now);
        user = users.save(user);

        UserEntitlementEntity entitlement = provisioningService.ensureFreeEntitlement(user.getUserId());
        assertNotNull(entitlement);
        assertEquals("FREE", entitlement.getSubscriptionPlan().getPlanCode().name());

        AuthSessionEntity session = sessions.save(AuthSessionEntity.create(user, "browser-entitle", "Test browser", Platform.WEB, now));
        String token = jwtService.issue(user.getUserId(), session.getSessionId(), user.getAuthzVersion()).value();

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.entitlement.planCode").value("FREE"))
                .andExpect(jsonPath("$.data.entitlement.allowanceLimit").value(30))
                .andExpect(jsonPath("$.data.entitlement.usedUnits").value(0))
                .andExpect(jsonPath("$.data.entitlement.remainingUnits").value(30))
                .andExpect(jsonPath("$.data.entitlement.cycleStartAt").exists())
                .andExpect(jsonPath("$.data.entitlement.cycleEndAt").exists());
    }
}
