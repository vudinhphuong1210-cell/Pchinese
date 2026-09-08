package net.pchinese.auth.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.pchinese.auth.application.AccountLifecycleService;
import net.pchinese.auth.application.SessionLifecycleService;
import net.pchinese.auth.domain.Platform;
import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.common.error.ApiException;
import net.pchinese.security.PchineseSecurityProperties;
import net.pchinese.security.UserPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final String LEGACY_REFRESH_COOKIE = "__Host-pchinese-refresh";
    private static final String REFRESH_COOKIE_PREFIX = "__Host-pchinese-refresh-";
    private static final String LEGACY_CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_COOKIE_PREFIX = "XSRF-TOKEN-";
    private static final String BROWSER_SESSION_HEADER = "X-Browser-Session-Id";
    private final AccountLifecycleService accounts;
    private final SessionLifecycleService sessions;
    private final PchineseSecurityProperties security;
    public AuthController(AccountLifecycleService accounts, SessionLifecycleService sessions, PchineseSecurityProperties security) {
        this.accounts = accounts; this.sessions = sessions; this.security = security;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiEnvelope<Map<String, Boolean>>> register(@Valid @RequestBody RegisterRequest request) {
        accounts.register(request.email(), request.password());
        return ResponseEntity.accepted().body(ApiEnvelope.success(Map.of("accepted", true)));
    }
    @PostMapping("/email-verifications")
    public ResponseEntity<ApiEnvelope<Map<String, Boolean>>> requestVerification(@Valid @RequestBody EmailRequest request) {
        accounts.requestVerification(request.email());
        return ResponseEntity.accepted().body(ApiEnvelope.success(Map.of("accepted", true)));
    }
    @PostMapping("/email-verifications/confirm")
    public ApiEnvelope<Map<String, String>> confirmVerification(@Valid @RequestBody VerificationTokenRequest request) {
        accounts.confirmVerification(request.verificationToken());
        return ApiEnvelope.success(Map.of("status", "ACTIVE"));
    }
    @PostMapping("/login")
    public ResponseEntity<ApiEnvelope<SessionLifecycleService.SessionPayload>> login(@Valid @RequestBody LoginRequest request) {
        SessionLifecycleService.BrowserSession session = sessions.login(request.email(), request.password(), request.deviceId(),
                request.deviceLabel(), request.platform());
        return withBrowserCookies(ResponseEntity.ok(), session, false);
    }
    @PostMapping("/refresh")
    public ResponseEntity<ApiEnvelope<SessionLifecycleService.SessionPayload>> refresh(HttpServletRequest request,
            @RequestHeader("X-Refresh-Request-Id") String requestId,
            @RequestHeader(value = BROWSER_SESSION_HEADER, required = false) String browserSessionId) {
        BrowserRefreshCookie refreshCookie = resolveRefreshCookie(request, browserSessionId);
        SessionLifecycleService.BrowserSession session = sessions.refresh(refreshCookie.value(), parseUuid(requestId), refreshCookie.sessionId());
        return withBrowserCookies(ResponseEntity.ok(), session, refreshCookie.legacy());
    }
    @PostMapping("/logout")
    public ResponseEntity<ApiEnvelope<Map<String, Object>>> logout(HttpServletRequest request,
            @RequestHeader(value = BROWSER_SESSION_HEADER, required = false) String browserSessionId) {
        BrowserRefreshCookie refreshCookie = resolveRefreshCookie(request, browserSessionId);
        sessions.logout(refreshCookie.value(), refreshCookie.sessionId());
        return clearBrowserCookies(ResponseEntity.ok(), refreshCookie).body(ApiEnvelope.success(Map.of()));
    }
    @PostMapping("/password-resets")
    public ResponseEntity<ApiEnvelope<Map<String, Boolean>>> requestPasswordReset(@Valid @RequestBody EmailRequest request) {
        accounts.requestPasswordReset(request.email());
        return ResponseEntity.accepted().body(ApiEnvelope.success(Map.of("accepted", true)));
    }
    @PostMapping("/password-resets/confirm")
    public ApiEnvelope<Map<String, Object>> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        accounts.confirmPasswordReset(request.resetToken(), request.newPassword());
        return ApiEnvelope.success(Map.of());
    }

    private ResponseEntity<ApiEnvelope<SessionLifecycleService.SessionPayload>> withBrowserCookies(
            ResponseEntity.BodyBuilder response, SessionLifecycleService.BrowserSession session, boolean clearLegacyCookies) {
        UUID browserSessionId = session.session().browserSessionId();
        response.header(HttpHeaders.SET_COOKIE, refreshCookie(browserSessionId, session.refreshToken()).toString())
                .header(HttpHeaders.SET_COOKIE, csrfCookie(browserSessionId, session.csrfToken()).toString());
        if (clearLegacyCookies) {
            response.header(HttpHeaders.SET_COOKIE, legacyRefreshCookie("", Duration.ZERO).toString())
                    .header(HttpHeaders.SET_COOKIE, legacyCsrfCookie("", Duration.ZERO).toString());
        }
        return response.body(ApiEnvelope.success(session.session()));
    }
    private ResponseEntity.BodyBuilder clearBrowserCookies(ResponseEntity.BodyBuilder response, BrowserRefreshCookie refreshCookie) {
        if (refreshCookie.legacy()) {
            return response.header(HttpHeaders.SET_COOKIE, legacyRefreshCookie("", Duration.ZERO).toString())
                    .header(HttpHeaders.SET_COOKIE, legacyCsrfCookie("", Duration.ZERO).toString());
        }
        return response.header(HttpHeaders.SET_COOKIE, refreshCookie(refreshCookie.sessionId(), "", Duration.ZERO).toString())
                .header(HttpHeaders.SET_COOKIE, csrfCookie(refreshCookie.sessionId(), "", Duration.ZERO).toString());
    }
    private ResponseCookie refreshCookie(UUID browserSessionId, String value) { return refreshCookie(browserSessionId, value, Duration.ofDays(30)); }
    private ResponseCookie refreshCookie(UUID browserSessionId, String value, Duration maxAge) {
        return ResponseCookie.from(refreshCookieName(browserSessionId), value).httpOnly(true).secure(security.isCookieSecure()).sameSite("Strict").path("/")
                .maxAge(maxAge).build();
    }
    private ResponseCookie csrfCookie(UUID browserSessionId, String value) { return csrfCookie(browserSessionId, value, Duration.ofDays(30)); }
    private ResponseCookie csrfCookie(UUID browserSessionId, String value, Duration maxAge) {
        return ResponseCookie.from(csrfCookieName(browserSessionId), value).httpOnly(false).secure(security.isCookieSecure()).sameSite("Strict").path("/")
                .maxAge(maxAge).build();
    }
    private ResponseCookie legacyRefreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(LEGACY_REFRESH_COOKIE, value).httpOnly(true).secure(security.isCookieSecure()).sameSite("Strict").path("/")
                .maxAge(maxAge).build();
    }
    private ResponseCookie legacyCsrfCookie(String value, Duration maxAge) {
        return ResponseCookie.from(LEGACY_CSRF_COOKIE, value).httpOnly(false).secure(security.isCookieSecure()).sameSite("Strict").path("/")
                .maxAge(maxAge).build();
    }
    private BrowserRefreshCookie resolveRefreshCookie(HttpServletRequest request, String requestedSessionId) {
        if (requestedSessionId != null && !requestedSessionId.isBlank()) {
            UUID sessionId = parseBrowserSessionId(requestedSessionId);
            return new BrowserRefreshCookie(sessionId, requiredCookie(request, refreshCookieName(sessionId)), false);
        }
        List<BrowserRefreshCookie> namedCookies = namedRefreshCookies(request);
        if (namedCookies.size() == 1) {
            return namedCookies.getFirst();
        }
        if (namedCookies.size() > 1) {
            throw ApiException.refreshInvalid();
        }
        return new BrowserRefreshCookie(null, requiredCookie(request, LEGACY_REFRESH_COOKIE), true);
    }
    private List<BrowserRefreshCookie> namedRefreshCookies(HttpServletRequest request) {
        List<BrowserRefreshCookie> result = new ArrayList<>();
        if (request.getCookies() == null) return result;
        for (Cookie cookie : request.getCookies()) {
            UUID sessionId = sessionIdFromCookieName(cookie.getName());
            if (sessionId != null) result.add(new BrowserRefreshCookie(sessionId, cookie.getValue(), false));
        }
        return result;
    }
    private UUID sessionIdFromCookieName(String name) {
        if (name == null || !name.startsWith(REFRESH_COOKIE_PREFIX)) return null;
        try { return UUID.fromString(name.substring(REFRESH_COOKIE_PREFIX.length())); }
        catch (IllegalArgumentException ignored) { return null; }
    }
    private UUID parseBrowserSessionId(String value) {
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException exception) { throw ApiException.refreshInvalid(); }
    }
    private String refreshCookieName(UUID browserSessionId) { return REFRESH_COOKIE_PREFIX + browserSessionId; }
    private String csrfCookieName(UUID browserSessionId) { return CSRF_COOKIE_PREFIX + browserSessionId; }
    private String requiredCookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null) for (Cookie cookie : request.getCookies()) if (name.equals(cookie.getName())) return cookie.getValue();
        throw ApiException.refreshInvalid();
    }
    private UUID parseUuid(String value) { try { return UUID.fromString(value); } catch (IllegalArgumentException exception) { throw ApiException.validation("X-Refresh-Request-Id must be a UUID."); } }

    public record RegisterRequest(@NotBlank @Email @Size(max = 320) String email, @NotBlank @Size(max = 128) String password) { }
    public record EmailRequest(@NotBlank @Email @Size(max = 320) String email) { }
    public record VerificationTokenRequest(@NotBlank @Size(max = 512) String verificationToken) { }
    public record LoginRequest(@NotBlank @Email @Size(max = 320) String email, @NotBlank @Size(max = 128) String password,
                               @NotBlank @Size(max = 128) String deviceId, @NotBlank @Size(max = 120) String deviceLabel,
                               @NotNull Platform platform) { }
    public record PasswordResetConfirmRequest(@NotBlank @Size(max = 512) String resetToken, @NotBlank @Size(max = 128) String newPassword) { }
    private record BrowserRefreshCookie(UUID sessionId, String value, boolean legacy) { }
}
