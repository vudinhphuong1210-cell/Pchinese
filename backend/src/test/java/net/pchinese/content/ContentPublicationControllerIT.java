package net.pchinese.content;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContentPublicationControllerIT {

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
    void publishTopicPrerequisitesValidation() throws Exception {
        AuthenticatedUser admin = activeUser("admin-pub", true);

        // Create topic
        String topicResp = mockMvc.perform(post("/api/v1/topics").header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Culture & Life","slug":"culture-life","description":"Desc","hskLevel":2,"sortOrder":1}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String topicId = topicResp.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        // Publish topic
        mockMvc.perform(post("/api/v1/topics/" + topicId + "/publish").header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicationStatus").value("PUBLISHED"));
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
