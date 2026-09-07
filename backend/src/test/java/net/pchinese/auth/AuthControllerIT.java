package net.pchinese.auth;

import jakarta.servlet.http.Cookie;
import net.pchinese.auth.application.AuthActionTokenDelivery;
import net.pchinese.auth.domain.ActionTokenPurpose;
import net.pchinese.security.SensitiveValueService;
import net.pchinese.users.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthControllerIT.TokenDeliveryConfig.class)
class AuthControllerIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @Autowired MockMvc mockMvc;
    @Autowired CapturingDelivery delivery;
    @Autowired UserRepository users;
    @Autowired SensitiveValueService sensitiveValues;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void registrationAlwaysUsesTheNeutralAcceptedEnvelope() throws Exception {
        String payload = "{\"email\":\"learner@example.test\",\"password\":\"StrongPassword123\"}";
        mockMvc.perform(post("/api/v1/auth/register").contentType("application/json").content(payload))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accepted").value(true)).andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.meta.correlationId").isNotEmpty());
        mockMvc.perform(post("/api/v1/auth/register").contentType("application/json").content(payload))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.data.accepted").value(true));
    }

    @Test
    void verificationLoginRefreshReplayReuseAndNoSessionManagementPathFollowTheContract() throws Exception {
        String email = "journey-" + UUID.randomUUID() + "@example.test";
        String password = "StrongPassword123";
        mockMvc.perform(post("/api/v1/auth/register").contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isAccepted());
        UUID userId = users.findByEmailLookupHash(sensitiveValues.hashEmail(email)).orElseThrow().getUserId();
        String verificationToken = delivery.token(userId, ActionTokenPurpose.EMAIL_VERIFICATION);
        mockMvc.perform(post("/api/v1/auth/email-verifications/confirm").contentType("application/json")
                .content("{\"verificationToken\":\"" + verificationToken + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ACTIVE"));

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json").content("""
                {"email":"%s","password":"%s","deviceId":"browser-1","deviceLabel":"Test browser","platform":"WEB"}
                """.formatted(email, password))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").doesNotExist()).andReturn();
        Cookie originalRefresh = login.getResponse().getCookie("__Host-pchinese-refresh");
        Cookie csrf = login.getResponse().getCookie("XSRF-TOKEN");
        String originalAccess = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");
        mockMvc.perform(get("/api/v1/auth/sessions").header("Authorization", "Bearer " + originalAccess))
                .andExpect(status().isNotFound());
        String requestId = UUID.randomUUID().toString();
        MvcResult firstRefresh = refresh(originalRefresh, csrf, requestId).andExpect(status().isOk()).andReturn();
        MvcResult replay = refresh(originalRefresh, csrf, requestId).andExpect(status().isOk()).andReturn();
        String refreshedAccess = com.jayway.jsonpath.JsonPath.read(firstRefresh.getResponse().getContentAsString(), "$.data.accessToken");
        String replayAccess = com.jayway.jsonpath.JsonPath.read(replay.getResponse().getContentAsString(), "$.data.accessToken");
        org.junit.jupiter.api.Assertions.assertEquals(refreshedAccess, replayAccess);
        refresh(originalRefresh, csrf, UUID.randomUUID().toString()).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_INVALID"));
    }

    @Test
    void malformedPublicRequestsHaveTheStandardValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private org.springframework.test.web.servlet.ResultActions refresh(Cookie refresh, Cookie csrf, String requestId) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").cookie(refresh, csrf).header("Origin", "http://localhost:3000")
                .header("X-CSRF-Token", csrf.getValue()).header("X-Refresh-Request-Id", requestId));
    }

    @TestConfiguration
    static class TokenDeliveryConfig {
        @Bean @Primary CapturingDelivery authActionTokenDelivery() { return new CapturingDelivery(); }
    }
    static class CapturingDelivery implements AuthActionTokenDelivery {
        private final Map<String, String> tokens = new ConcurrentHashMap<>();
        @Override public void deliver(UUID userId, ActionTokenPurpose purpose, String rawToken) { tokens.put(userId + ":" + purpose, rawToken); }
        String token(UUID userId, ActionTokenPurpose purpose) { return tokens.get(userId + ":" + purpose); }
    }
}
