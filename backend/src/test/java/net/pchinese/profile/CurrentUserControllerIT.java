package net.pchinese.profile;

import net.pchinese.auth.domain.Platform;
import net.pchinese.auth.persistence.AuthSessionEntity;
import net.pchinese.auth.persistence.AuthSessionRepository;
import net.pchinese.security.JwtService;
import net.pchinese.security.SensitiveValueService;
import net.pchinese.users.persistence.UserEntity;
import net.pchinese.users.persistence.UserRepository;
import net.pchinese.users.persistence.UserRoleEntity;
import net.pchinese.users.persistence.UserRoleRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CurrentUserControllerIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired UserRoleRepository roles;
    @Autowired AuthSessionRepository sessions;
    @Autowired JwtService jwtService;
    @Autowired SensitiveValueService sensitiveValues;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void currentUserProfileIsPrivateAndValidUpdatesReturnTheCanonicalProjection() throws Exception {
        AuthenticatedUser learner = activeUser("learner", false);
        UserEntity persisted = users.findById(learner.user().getUserId()).orElseThrow();

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + learner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profile.nativeLanguageCode").value("vi"))
                .andExpect(jsonPath("$.data.entitlement.planCode").value("FREE"))
                .andExpect(jsonPath("$.data.entitlement.allowanceLimit").value(30))
                .andExpect(jsonPath("$.data.entitlement.remainingUnits").value(30));

        mockMvc.perform(patch("/api/v1/me").header("Authorization", "Bearer " + learner.accessToken())
                        .contentType("application/json").content("""
                                {"displayName":"Nguyen Van A","nativeLanguageCode":"en","interfaceLocale":"en-US",
                                 "timeZone":"Asia/Tokyo","targetHskLevel":6,"dailyGoalMinutes":60,
                                 "expectedProfileVersion":%d}
                                """.formatted(persisted.getVersion())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.displayName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.data.profile.targetHskLevel").value(6))
                .andExpect(jsonPath("$.data.profile.dailyGoalMinutes").value(60));

        UserEntity updated = users.findById(learner.user().getUserId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("Nguyen Van A",
                sensitiveValues.decryptToString(updated.getDisplayNameCiphertext()));
    }

    @Test
    void staleOrUnsupportedUpdatesAreRejectedWithoutChangingSavedPreferences() throws Exception {
        AuthenticatedUser learner = activeUser("stale", false);
        UserEntity before = users.findById(learner.user().getUserId()).orElseThrow();
        long staleVersion = before.getVersion();

        mockMvc.perform(patch("/api/v1/me").header("Authorization", "Bearer " + learner.accessToken())
                        .contentType("application/json").content("""
                                {"displayName":"First value","targetHskLevel":4,"dailyGoalMinutes":30,
                                 "expectedProfileVersion":%d}
                                """.formatted(staleVersion)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/me").header("Authorization", "Bearer " + learner.accessToken())
                        .contentType("application/json").content("""
                                {"displayName":"Overwritten","targetHskLevel":5,"dailyGoalMinutes":45,
                                 "expectedProfileVersion":%d}
                                """.formatted(staleVersion)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.error.code").value("STATE_CONFLICT"));

        mockMvc.perform(patch("/api/v1/me").header("Authorization", "Bearer " + learner.accessToken())
                        .contentType("application/json").content("""
                                {"displayName":"Invalid","targetHskLevel":7,"dailyGoalMinutes":45,
                                 "expectedProfileVersion":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        mockMvc.perform(patch("/api/v1/me").header("Authorization", "Bearer " + learner.accessToken())
                        .contentType("application/json").content("""
                                {"displayName":"Invalid","roles":["ADMIN"],"expectedProfileVersion":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + learner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.displayName").value("First value"))
                .andExpect(jsonPath("$.data.profile.targetHskLevel").value(4))
                .andExpect(jsonPath("$.data.profile.dailyGoalMinutes").value(30));
    }

    @Test
    void anAdminCanReadOnlyItsOwnProfileAndLockedAccountsAreNeverExposed() throws Exception {
        AuthenticatedUser learner = activeUser("learner-private", false);
        AuthenticatedUser admin = activeUser("admin-private", true);

        mockMvc.perform(patch("/api/v1/me").header("Authorization", "Bearer " + learner.accessToken())
                        .contentType("application/json").content("""
                                {"displayName":"Learner private name","expectedProfileVersion":%d}
                                """.formatted(learner.user().getVersion())))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/me").header("Authorization", "Bearer " + admin.accessToken())
                        .contentType("application/json").content("""
                                {"displayName":"Admin own name","expectedProfileVersion":%d}
                                """.formatted(admin.user().getVersion())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.displayName").value("Admin own name"));

        UserEntity lockedLearner = users.findById(learner.user().getUserId()).orElseThrow();
        lockedLearner.lock(Instant.now());
        users.save(lockedLearner);
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + learner.accessToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    private AuthenticatedUser activeUser(String prefix, boolean admin) {
        Instant now = Instant.now();
        String lookupHash = (prefix + UUID.randomUUID()).replace("-", "");
        UserEntity user = users.save(UserEntity.pending(new byte[] {1}, (lookupHash + "0".repeat(64)).substring(0, 64), "hash", now));
        user.activate(now);
        user = users.save(user);
        if (admin) roles.save(UserRoleEntity.grant(user, null, UUID.randomUUID(), now));
        AuthSessionEntity session = sessions.save(AuthSessionEntity.create(user, "browser-" + prefix, "Test browser", Platform.WEB, now));
        String accessToken = jwtService.issue(user.getUserId(), session.getSessionId(), user.getAuthzVersion()).value();
        return new AuthenticatedUser(user, accessToken);
    }

    private record AuthenticatedUser(UserEntity user, String accessToken) { }
}
