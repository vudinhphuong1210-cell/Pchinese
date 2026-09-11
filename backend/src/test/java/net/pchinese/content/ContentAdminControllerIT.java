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
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContentAdminControllerIT {

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
    void nonAdminGetsForbiddenOnWriteAndDraftReadRoutes() throws Exception {
        AuthenticatedUser learner = activeUser("learner-f04", false);

        mockMvc.perform(get("/api/v1/admin/content/topics").header("Authorization", "Bearer " + learner.accessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AUTHORIZATION_DENIED"));

        mockMvc.perform(post("/api/v1/topics").header("Authorization", "Bearer " + learner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Topic 1","slug":"topic-1","description":"Desc","hskLevel":1,"sortOrder":1}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("AUTHORIZATION_DENIED"));
    }

    @Test
    void adminCanCreateAndListTopics() throws Exception {
        AuthenticatedUser admin = activeUser("admin-f04", true);

        mockMvc.perform(post("/api/v1/topics").header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"HSK 1 Stories","slug":"hsk1-stories","description":"Fun stories","hskLevel":1,"sortOrder":0}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("hsk1-stories"))
                .andExpect(jsonPath("$.data.publicationStatus").value("DRAFT"));

        mockMvc.perform(get("/api/v1/admin/content/topics").header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].entityType").value("TOPIC"))
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.size").value(50));
    }

    @Test
    void adminCanRegisterYoutubeMediaFromJsonAndOnlyReturnsTheCanonicalId() throws Exception {
        AuthenticatedUser admin = activeUser("admin-media-f04", true);
        mockMvc.perform(post("/api/v1/media").header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"youtubeVideoReference":"https://youtu.be/dQw4w9WgXcQ?t=43","title":"Safe video"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.entityType").value("MEDIA"))
                .andExpect(jsonPath("$.data.publicationStatus").value("PENDING_SCAN"))
                .andExpect(jsonPath("$.data.approvalEligible").value(false))
                .andExpect(jsonPath("$.data.providerName").value("YOUTUBE"))
                .andExpect(jsonPath("$.data.youtubeVideoId").value("dQw4w9WgXcQ"));
    }

    @Test
    void mediaSourceFieldsCannotBeChangedAndMultipartUploadsAreRejected() throws Exception {
        AuthenticatedUser admin = activeUser("admin-media-source-f04", true);
        String accessToken = "Bearer " + admin.accessToken();
        String mediaId = com.jayway.jsonpath.JsonPath.read(mockMvc.perform(post("/api/v1/media")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"youtubeVideoReference":"9bZkp7q19f0","title":"Safe video"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(patch("/api/v1/media/{mediaAssetId}", mediaId)
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"youtubeVideoReference":"3JZ_D3ELwOQ","expectedVersion":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/admin/content/media/{mediaAssetId}", mediaId)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.youtubeVideoId").value("9bZkp7q19f0"));

        MockMultipartFile file = new MockMultipartFile("file", "lesson.mp4", "video/mp4", new byte[] {1});
        mockMvc.perform(multipart("/api/v1/media").file(file).header("Authorization", accessToken))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void validationErrorsUseTheStandardSafeEnvelope() throws Exception {
        AuthenticatedUser admin = activeUser("admin-invalid-f04", true);

        mockMvc.perform(post("/api/v1/topics").header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"slug\":\"not valid\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
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
