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
                .andExpect(jsonPath("$.data.browserSessionId").isNotEmpty())
                .andExpect(jsonPath("$.data.roles").isEmpty()).andReturn();
        String browserSessionId = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.data.browserSessionId");
        Cookie originalRefresh = login.getResponse().getCookie("__Host-pchinese-refresh-" + browserSessionId);
        Cookie csrf = login.getResponse().getCookie("XSRF-TOKEN-" + browserSessionId);
        String originalAccess = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");
        mockMvc.perform(get("/api/v1/auth/sessions").header("Authorization", "Bearer " + originalAccess))
                .andExpect(status().isNotFound());

        // A browser upgraded from the old single-cookie design can refresh once
        // without a selector. The server returns the new per-session cookies.
        MvcResult legacyMigration = refreshLegacy(
                new Cookie("__Host-pchinese-refresh", originalRefresh.getValue()),
                new Cookie("XSRF-TOKEN", csrf.getValue()), UUID.randomUUID().toString())
                .andExpect(status().isOk()).andReturn();
        originalRefresh = legacyMigration.getResponse().getCookie("__Host-pchinese-refresh-" + browserSessionId);
        csrf = legacyMigration.getResponse().getCookie("XSRF-TOKEN-" + browserSessionId);
        org.junit.jupiter.api.Assertions.assertNotNull(originalRefresh);
        org.junit.jupiter.api.Assertions.assertNotNull(csrf);

        String requestId = UUID.randomUUID().toString();
        MvcResult firstRefresh = refresh(browserSessionId, originalRefresh, csrf, requestId).andExpect(status().isOk()).andReturn();
        MvcResult replay = refresh(browserSessionId, originalRefresh, csrf, requestId).andExpect(status().isOk()).andReturn();
        String refreshedAccess = com.jayway.jsonpath.JsonPath.read(firstRefresh.getResponse().getContentAsString(), "$.data.accessToken");
        String replayAccess = com.jayway.jsonpath.JsonPath.read(replay.getResponse().getContentAsString(), "$.data.accessToken");
        org.junit.jupiter.api.Assertions.assertEquals(refreshedAccess, replayAccess);
        refresh(browserSessionId, originalRefresh, csrf, UUID.randomUUID().toString()).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_INVALID"));
    }

    @Test
    void separateAccountTabsKeepRefreshAndLogoutIsolated() throws Exception {
        LoginFixture accountA = activeLogin("multi-account-a");
        LoginFixture accountB = activeLogin("multi-account-b");
        org.junit.jupiter.api.Assertions.assertNotEquals(accountA.browserSessionId(), accountB.browserSessionId());

        MvcResult aRefresh = refresh(accountA.browserSessionId(), accountA.refresh(), accountA.csrf(), UUID.randomUUID().toString(),
                accountB.refresh(), accountB.csrf()).andExpect(status().isOk()).andReturn();
        Cookie rotatedA = aRefresh.getResponse().getCookie("__Host-pchinese-refresh-" + accountA.browserSessionId());
        Cookie rotatedCsrfA = aRefresh.getResponse().getCookie("XSRF-TOKEN-" + accountA.browserSessionId());
        org.junit.jupiter.api.Assertions.assertNotNull(rotatedA);
        org.junit.jupiter.api.Assertions.assertNotNull(rotatedCsrfA);

        MvcResult bRefresh = refresh(accountB.browserSessionId(), accountB.refresh(), accountB.csrf(), UUID.randomUUID().toString(),
                rotatedA, rotatedCsrfA).andExpect(status().isOk()).andReturn();
        Cookie rotatedB = bRefresh.getResponse().getCookie("__Host-pchinese-refresh-" + accountB.browserSessionId());
        Cookie rotatedCsrfB = bRefresh.getResponse().getCookie("XSRF-TOKEN-" + accountB.browserSessionId());
        org.junit.jupiter.api.Assertions.assertNotNull(rotatedB);
        org.junit.jupiter.api.Assertions.assertNotNull(rotatedCsrfB);

        mockMvc.perform(post("/api/v1/auth/logout").cookie(rotatedA, rotatedCsrfA, rotatedB, rotatedCsrfB)
                .header("Origin", "http://localhost:3000").header("X-CSRF-Token", rotatedCsrfB.getValue())
                .header("X-Browser-Session-Id", accountB.browserSessionId()))
                .andExpect(status().isOk());

        refresh(accountA.browserSessionId(), rotatedA, rotatedCsrfA, UUID.randomUUID().toString())
                .andExpect(status().isOk());
    }

    @Test
    void selectorCannotBeUsedToRouteToAnotherAccountsCookie() throws Exception {
        LoginFixture account = activeLogin("mismatched-selector");
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(account.refresh(), account.csrf())
                .header("Origin", "http://localhost:3000").header("X-CSRF-Token", account.csrf().getValue())
                .header("X-Refresh-Request-Id", UUID.randomUUID().toString())
                .header("X-Browser-Session-Id", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void malformedPublicRequestsHaveTheStandardValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private LoginFixture activeLogin(String deviceId) throws Exception {
        String email = "multi-" + UUID.randomUUID() + "@example.test";
        String password = "StrongPassword123";
        mockMvc.perform(post("/api/v1/auth/register").contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isAccepted());
        UUID userId = users.findByEmailLookupHash(sensitiveValues.hashEmail(email)).orElseThrow().getUserId();
        mockMvc.perform(post("/api/v1/auth/email-verifications/confirm").contentType("application/json")
                .content("{\"verificationToken\":\"" + delivery.token(userId, ActionTokenPurpose.EMAIL_VERIFICATION) + "\"}"))
                .andExpect(status().isOk());
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json").content("""
                {"email":"%s","password":"%s","deviceId":"%s","deviceLabel":"Test browser","platform":"WEB"}
                """.formatted(email, password, deviceId))).andExpect(status().isOk()).andReturn();
        String browserSessionId = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.data.browserSessionId");
        Cookie refresh = login.getResponse().getCookie("__Host-pchinese-refresh-" + browserSessionId);
        Cookie csrf = login.getResponse().getCookie("XSRF-TOKEN-" + browserSessionId);
        org.junit.jupiter.api.Assertions.assertNotNull(refresh);
        org.junit.jupiter.api.Assertions.assertNotNull(csrf);
        return new LoginFixture(browserSessionId, refresh, csrf);
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String browserSessionId, Cookie refresh, Cookie csrf, String requestId,
                                                                        Cookie... additionalCookies) throws Exception {
        Cookie[] cookies = new Cookie[2 + additionalCookies.length];
        cookies[0] = refresh;
        cookies[1] = csrf;
        System.arraycopy(additionalCookies, 0, cookies, 2, additionalCookies.length);
        return mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookies).header("Origin", "http://localhost:3000")
                .header("X-CSRF-Token", csrf.getValue()).header("X-Refresh-Request-Id", requestId)
                .header("X-Browser-Session-Id", browserSessionId));
    }

    private org.springframework.test.web.servlet.ResultActions refreshLegacy(Cookie refresh, Cookie csrf, String requestId) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").cookie(refresh, csrf).header("Origin", "http://localhost:3000")
                .header("X-CSRF-Token", csrf.getValue()).header("X-Refresh-Request-Id", requestId));
    }

    private record LoginFixture(String browserSessionId, Cookie refresh, Cookie csrf) { }

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
