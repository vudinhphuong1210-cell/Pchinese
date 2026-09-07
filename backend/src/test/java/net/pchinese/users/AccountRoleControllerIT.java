package net.pchinese.users;

import net.pchinese.auth.domain.Platform;
import net.pchinese.auth.persistence.AuthSessionEntity;
import net.pchinese.auth.persistence.AuthSessionRepository;
import net.pchinese.security.JwtService;
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
    @Autowired JwtService jwtService;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void accountRoleEndpointsRejectUnauthenticatedCallersWithoutTargetDisclosure() throws Exception {
        mockMvc.perform(get("/api/v1/users/550e8400-e29b-41d4-a716-446655440000/roles"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void exactIdAdminCommandsReturnOnlyRoleAccessAndEnforceSelfGuard() throws Exception {
        UserEntity actor = activeUser("actor");
        roles.save(UserRoleEntity.grant(actor, null, UUID.randomUUID(), Instant.now()));
        AuthSessionEntity actorSession = sessions.save(AuthSessionEntity.create(actor, "admin-browser", "Admin browser", Platform.WEB, Instant.now()));
        String access = jwtService.issue(actor.getUserId(), actorSession.getSessionId(), actor.getAuthzVersion()).value();
        UserEntity target = activeUser("target");
        String targetPath = "/api/v1/users/" + target.getUserId();

        mockMvc.perform(get(targetPath + "/roles").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.userId").value(target.getUserId().toString()))
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

    private UserEntity activeUser(String prefix) {
        Instant now = Instant.now();
        String hash = (prefix + UUID.randomUUID()).replace("-", "");
        UserEntity user = users.save(UserEntity.pending(new byte[] {1}, (hash + "0".repeat(64)).substring(0, 64), "hash", now));
        user.activate(now);
        return users.save(user);
    }
}
