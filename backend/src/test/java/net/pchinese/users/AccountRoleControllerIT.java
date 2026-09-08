package net.pchinese.users;

import net.pchinese.auth.domain.Platform;
import net.pchinese.auth.persistence.AuthSessionEntity;
import net.pchinese.auth.persistence.AuthSessionRepository;
import net.pchinese.auth.persistence.AuthAuditEventEntity;
import net.pchinese.auth.persistence.AuthAuditEventRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountRoleControllerIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired UserRoleRepository roles;
    @Autowired AuthSessionRepository sessions;
    @Autowired AuthAuditEventRepository auditEvents;
    @Autowired JwtService jwtService;
    @Autowired SensitiveValueService sensitiveValues;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void accountRoleEndpointsRejectUnauthenticatedCallersWithoutTargetDisclosure() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        mockMvc.perform(get("/api/v1/users/550e8400-e29b-41d4-a716-446655440000/roles"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void userDirectoryIsAdminOnlyPagedAndNeverReturnsPrivateFields() throws Exception {
        UserEntity actor = activeUser("directory-actor");
        roles.save(UserRoleEntity.grant(actor, null, UUID.randomUUID(), Instant.now()));
        AuthSessionEntity actorSession = sessions.save(AuthSessionEntity.create(actor, "admin-directory", "Admin directory", Platform.WEB, Instant.now()));
        String adminAccess = jwtService.issue(actor.getUserId(), actorSession.getSessionId(), actor.getAuthzVersion()).value();
        activeUser("directory-target", "Người học quản trị");

        mockMvc.perform(get("/api/v1/users?page=0&size=1").header("Authorization", "Bearer " + adminAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].userId").exists())
                .andExpect(jsonPath("$.data.items[0].accountName").value("Người học quản trị"))
                .andExpect(jsonPath("$.data.items[0].accountState").exists())
                .andExpect(jsonPath("$.data.items[0].roles").isArray())
                .andExpect(jsonPath("$.data.items[0].email").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].displayName").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].profile").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].sessions").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].entitlement").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].learningData").doesNotExist())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2));

        mockMvc.perform(get("/api/v1/users?page=-1&size=51").header("Authorization", "Bearer " + adminAccess))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        UserEntity learner = activeUser("directory-learner");
        AuthSessionEntity learnerSession = sessions.save(AuthSessionEntity.create(learner, "learner-directory", "Learner directory", Platform.WEB, Instant.now()));
        String learnerAccess = jwtService.issue(learner.getUserId(), learnerSession.getSessionId(), learner.getAuthzVersion()).value();
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + learnerAccess))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("AUTHORIZATION_DENIED"));
    }

    @Test
    void exactIdAdminCommandsReturnOnlyRoleAccessAndEnforceSelfGuard() throws Exception {
        UserEntity actor = activeUser("actor");
        roles.save(UserRoleEntity.grant(actor, null, UUID.randomUUID(), Instant.now()));
        AuthSessionEntity actorSession = sessions.save(AuthSessionEntity.create(actor, "admin-browser", "Admin browser", Platform.WEB, Instant.now()));
        String access = jwtService.issue(actor.getUserId(), actorSession.getSessionId(), actor.getAuthzVersion()).value();
        UserEntity target = activeUser("target", "Tài khoản cần quản lý");
        String targetPath = "/api/v1/users/" + target.getUserId();

        mockMvc.perform(get(targetPath + "/roles").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.userId").value(target.getUserId().toString()))
                .andExpect(jsonPath("$.data.accountName").value("Tài khoản cần quản lý"))
                .andExpect(jsonPath("$.data.roles").isArray()).andExpect(jsonPath("$.data.accessState").value("ACTIVE"));
        mockMvc.perform(post(targetPath + "/roles").header("Authorization", "Bearer " + access)
                .contentType("application/json").content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.roles[0]").value("ADMIN"));
        mockMvc.perform(delete(targetPath + "/roles/ADMIN").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.roles").isEmpty());
        mockMvc.perform(post(targetPath + "/lock").header("Authorization", "Bearer " + access)
                .contentType("application/json").content("{\"reason\":\"OTHER\",\"note\":\"Security review\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.accessState").value("LOCKED"));
        mockMvc.perform(post(targetPath + "/unlock").header("Authorization", "Bearer " + access)
                .contentType("application/json").content("{\"reason\":\"SECURITY\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.accessState").value("ACTIVE"));
        mockMvc.perform(delete("/api/v1/users/" + actor.getUserId() + "/roles/ADMIN").header("Authorization", "Bearer " + access))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("STATE_CONFLICT"));
    }

    @Test
    void systemActivityIsAdminOnlyAndExcludesAuditPrivateMetadata() throws Exception {
        UserEntity admin = activeUser("audit-admin", "Audit Admin");
        roles.save(UserRoleEntity.grant(admin, null, UUID.randomUUID(), Instant.now()));
        AuthSessionEntity adminSession = sessions.save(AuthSessionEntity.create(admin, "admin-audit", "Admin audit", Platform.WEB, Instant.now()));
        String adminAccess = jwtService.issue(admin.getUserId(), adminSession.getSessionId(), admin.getAuthzVersion()).value();
        UserEntity learner = activeUser("audit-learner", "Audit Learner");
        auditEvents.save(AuthAuditEventEntity.create("PROFILE_PREFERENCES_UPDATED", learner.getUserId(), learner.getUserId(),
                UUID.randomUUID(), null, null, UUID.randomUUID(),
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode().put("secret", "hidden"),
                Instant.parse("2026-09-09T08:00:00Z")));

        mockMvc.perform(get("/api/v1/admin/audit-events?page=0&size=20").header("Authorization", "Bearer " + adminAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].eventType").value("PROFILE_PREFERENCES_UPDATED"))
                .andExpect(jsonPath("$.data.items[0].actorAccountName").value("Audit Learner"))
                .andExpect(jsonPath("$.data.items[0].targetAccountName").value("Audit Learner"))
                .andExpect(jsonPath("$.data.items[0].occurredAt").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].actorUserId").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].targetUserId").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].details").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].sessionId").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].correlationId").doesNotExist());

        AuthSessionEntity learnerSession = sessions.save(AuthSessionEntity.create(learner, "learner-audit", "Learner audit", Platform.WEB, Instant.now()));
        String learnerAccess = jwtService.issue(learner.getUserId(), learnerSession.getSessionId(), learner.getAuthzVersion()).value();
        mockMvc.perform(get("/api/v1/admin/audit-events").header("Authorization", "Bearer " + learnerAccess))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("AUTHORIZATION_DENIED"));
        mockMvc.perform(get("/api/v1/admin/audit-events?page=-1").header("Authorization", "Bearer " + adminAccess))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private UserEntity activeUser(String prefix) {
        return activeUser(prefix, null);
    }

    private UserEntity activeUser(String prefix, String accountName) {
        Instant now = Instant.now();
        String hash = (prefix + UUID.randomUUID()).replace("-", "");
        UserEntity user = users.save(UserEntity.pending(new byte[] {1}, (hash + "0".repeat(64)).substring(0, 64), "hash", now));
        user.activate(now);
        if (accountName != null) {
            user.updateProfile(sensitiveValues.encrypt(accountName), user.getNativeLanguageCode(), user.getInterfaceLocale(),
                    user.getTimeZone(), user.getTargetHskLevel(), user.getDailyGoalMinutes(), now);
        }
        return users.save(user);
    }
}
