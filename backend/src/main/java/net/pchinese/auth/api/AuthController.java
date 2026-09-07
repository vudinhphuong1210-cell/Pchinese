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
import java.util.Map;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "__Host-pchinese-refresh";
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
        return withBrowserCookies(ResponseEntity.ok(), session);
    }
    @PostMapping("/refresh")
    public ResponseEntity<ApiEnvelope<SessionLifecycleService.SessionPayload>> refresh(HttpServletRequest request,
            @RequestHeader("X-Refresh-Request-Id") String requestId) {
        SessionLifecycleService.BrowserSession session = sessions.refresh(requiredCookie(request, REFRESH_COOKIE), parseUuid(requestId));
        return withBrowserCookies(ResponseEntity.ok(), session);
    }
    @PostMapping("/logout")
    public ResponseEntity<ApiEnvelope<Map<String, Object>>> logout(HttpServletRequest request) {
        sessions.logout(requiredCookie(request, REFRESH_COOKIE));
        return clearBrowserCookies(ResponseEntity.ok()).body(ApiEnvelope.success(Map.of()));
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
            ResponseEntity.BodyBuilder response, SessionLifecycleService.BrowserSession session) {
        return response.header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken()).toString())
                .header(HttpHeaders.SET_COOKIE, csrfCookie(session.csrfToken()).toString())
                .body(ApiEnvelope.success(session.session()));
    }
    private ResponseEntity.BodyBuilder clearBrowserCookies(ResponseEntity.BodyBuilder response) {
        return response.header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .header(HttpHeaders.SET_COOKIE, csrfCookie("", Duration.ZERO).toString());
    }
    private ResponseCookie refreshCookie(String value) { return refreshCookie(value, Duration.ofDays(30)); }
    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value).httpOnly(true).secure(security.isCookieSecure()).sameSite("Strict").path("/")
                .maxAge(maxAge).build();
    }
    private ResponseCookie csrfCookie(String value) { return csrfCookie(value, Duration.ofDays(30)); }
    private ResponseCookie csrfCookie(String value, Duration maxAge) {
        return ResponseCookie.from("XSRF-TOKEN", value).httpOnly(false).secure(security.isCookieSecure()).sameSite("Strict").path("/")
                .maxAge(maxAge).build();
    }
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
}
