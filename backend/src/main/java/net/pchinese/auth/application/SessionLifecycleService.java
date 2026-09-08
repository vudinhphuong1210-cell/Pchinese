package net.pchinese.auth.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.pchinese.auth.domain.Platform;
import net.pchinese.auth.persistence.AuthSessionEntity;
import net.pchinese.auth.persistence.AuthSessionRepository;
import net.pchinese.auth.persistence.RefreshIdempotencyEntity;
import net.pchinese.auth.persistence.RefreshIdempotencyRepository;
import net.pchinese.auth.persistence.RefreshTokenEntity;
import net.pchinese.auth.persistence.RefreshTokenRepository;
import net.pchinese.common.error.ApiException;
import net.pchinese.security.JwtService;
import net.pchinese.security.SensitiveValueService;
import net.pchinese.users.persistence.UserEntity;
import net.pchinese.users.persistence.UserRepository;
import net.pchinese.users.persistence.UserRoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SessionLifecycleService {
    private final UserRepository users;
    private final UserRoleRepository roles;
    private final AuthSessionRepository sessions;
    private final RefreshTokenRepository refreshTokens;
    private final RefreshIdempotencyRepository idempotency;
    private final PasswordEncoder passwordEncoder;
    private final SensitiveValueService sensitiveValues;
    private final JwtService jwtService;
    private final AuthAuditService audit;
    private final ObjectMapper objectMapper;

    public SessionLifecycleService(UserRepository users, UserRoleRepository roles, AuthSessionRepository sessions, RefreshTokenRepository refreshTokens,
                                   RefreshIdempotencyRepository idempotency, PasswordEncoder passwordEncoder,
                                   SensitiveValueService sensitiveValues, JwtService jwtService, AuthAuditService audit,
                                   ObjectMapper objectMapper) {
        this.users = users; this.roles = roles; this.sessions = sessions; this.refreshTokens = refreshTokens; this.idempotency = idempotency;
        this.passwordEncoder = passwordEncoder; this.sensitiveValues = sensitiveValues; this.jwtService = jwtService;
        this.audit = audit; this.objectMapper = objectMapper;
    }

    @Transactional
    public BrowserSession login(String email, String password, String deviceId, String deviceLabel, Platform platform) {
        UserEntity user = users.findByEmailLookupHash(sensitiveValues.hashEmail(email.trim().toLowerCase(java.util.Locale.ROOT)))
                .orElseThrow(ApiException::unauthenticated);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) throw ApiException.unauthenticated();
        if (!user.isActiveVerified()) throw ApiException.forbidden();
        Instant now = Instant.now();
        AuthSessionEntity session = sessions.save(AuthSessionEntity.create(user, deviceId, sanitizeDeviceLabel(deviceLabel), platform, now));
        String rawRefresh = sensitiveValues.randomOpaqueToken();
        refreshTokens.save(RefreshTokenEntity.create(session, sensitiveValues.hashToken(rawRefresh), now));
        JwtService.AccessToken access = jwtService.issue(user.getUserId(), session.getSessionId(), user.getAuthzVersion());
        audit.record(AuditEventTaxonomy.EventType.LOGIN, user.getUserId(), user.getUserId(), session.getSessionId(), null, null,
                audit.details(AuditEventTaxonomy.OutcomeCode.SUCCESS), now);
        return new BrowserSession(sessionPayload(user, access, session.getSessionId()), rawRefresh,
                sensitiveValues.randomOpaqueToken());
    }

    @Transactional
    public BrowserSession refresh(String rawRefresh, UUID requestId, UUID expectedBrowserSessionId) {
        Instant now = Instant.now();
        RefreshTokenEntity source = refreshTokens.findLockedByTokenHash(sensitiveValues.hashToken(rawRefresh))
                .orElseThrow(ApiException::refreshInvalid);
        AuthSessionEntity session = source.getSession();
        if (expectedBrowserSessionId != null && !expectedBrowserSessionId.equals(session.getSessionId())) {
            throw ApiException.refreshInvalid();
        }
        if (!source.isUsable(now)) {
            return replayOrInvalidateFamily(source, requestId, now);
        }
        UserEntity user = session.getUser();
        if (!session.isActive(now) || !user.isActiveVerified() || user.getAuthzVersion() != session.getAuthzVersion()) {
            revokeSessionEntity(session, "AUTHORIZATION_CHANGED", now);
            throw ApiException.refreshInvalid();
        }
        if (idempotency.findLiveForSessionRequest(session.getSessionId(), requestId, now).isPresent()) {
            revokeFamily(source.getFamilyId(), "REFRESH_REQUEST_ID_REUSE", now);
            audit.record(AuditEventTaxonomy.EventType.REFRESH_REUSE, user.getUserId(), user.getUserId(), session.getSessionId(), null, null,
                    audit.details(AuditEventTaxonomy.OutcomeCode.DENIED), now);
            throw ApiException.refreshInvalid();
        }
        String nextRawRefresh = sensitiveValues.randomOpaqueToken();
        RefreshTokenEntity next = RefreshTokenEntity.create(session, sensitiveValues.hashToken(nextRawRefresh), now);
        refreshTokens.save(next);
        source.rotateTo(next, now);
        session.seen(now);
        JwtService.AccessToken access = jwtService.issue(user.getUserId(), session.getSessionId(), user.getAuthzVersion());
        String csrf = sensitiveValues.randomOpaqueToken();
        BrowserSession result = new BrowserSession(sessionPayload(user, access, session.getSessionId()), nextRawRefresh, csrf);
        try {
            byte[] replay = sensitiveValues.encrypt(objectMapper.writeValueAsBytes(new RefreshReplay(
                    result.session().accessToken(), result.session().expiresAt(), result.session().roles(),
                    result.session().browserSessionId(), nextRawRefresh, csrf)));
            idempotency.save(RefreshIdempotencyEntity.create(session.getSessionId(), source.getRefreshTokenId(), requestId, replay, now));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to preserve refresh retry result.", exception);
        }
        return result;
    }

    @Transactional
    public void logout(String rawRefresh, UUID expectedBrowserSessionId) {
        RefreshTokenEntity token = refreshTokens.findLockedByTokenHash(sensitiveValues.hashToken(rawRefresh))
                .orElseThrow(ApiException::refreshInvalid);
        if (expectedBrowserSessionId != null && !expectedBrowserSessionId.equals(token.getSession().getSessionId())) {
            throw ApiException.refreshInvalid();
        }
        if (!token.isUsable(Instant.now())) throw ApiException.refreshInvalid();
        revokeSessionEntity(token.getSession(), "LOGOUT", Instant.now());
    }

    @Transactional
    public void revokeAllForUser(UUID userId, String reason, Instant now) {
        sessions.findActiveByUserId(userId, now).forEach(session -> revokeSessionEntity(session, reason, now));
    }

    private BrowserSession replayOrInvalidateFamily(RefreshTokenEntity source, UUID requestId, Instant now) {
        AuthSessionEntity session = source.getSession();
        if (!session.isActive(now) || !session.getUser().isActiveVerified()
                || session.getUser().getAuthzVersion() != session.getAuthzVersion()) {
            throw ApiException.refreshInvalid();
        }
        return idempotency.findLive(source.getSession().getSessionId(), source.getRefreshTokenId(), requestId, now)
                .map(entry -> decryptReplay(entry))
                .orElseGet(() -> {
                    revokeFamily(source.getFamilyId(), "REFRESH_REUSE", now);
                    audit.record(AuditEventTaxonomy.EventType.REFRESH_REUSE,
                            source.getSession().getUser().getUserId(), source.getSession().getUser().getUserId(),
                            source.getSession().getSessionId(), null, null,
                            audit.details(AuditEventTaxonomy.OutcomeCode.DENIED), now);
                    throw ApiException.refreshInvalid();
                });
    }
    private BrowserSession decryptReplay(RefreshIdempotencyEntity entry) {
        try {
            RefreshReplay replay = objectMapper.readValue(sensitiveValues.decrypt(entry.getResponseCiphertext()), RefreshReplay.class);
            return new BrowserSession(new SessionPayload(replay.accessToken(), replay.expiresAt(),
                    replay.roles() == null ? List.of() : replay.roles(), replay.browserSessionId()), replay.refreshToken(), replay.csrfToken());
        } catch (Exception exception) {
            throw ApiException.refreshInvalid();
        }
    }
    private void revokeFamily(UUID familyId, String reason, Instant now) {
        refreshTokens.findActiveByFamilyId(familyId).forEach(token -> token.revoke(reason, now));
        sessions.findActiveByFamilyId(familyId).forEach(session -> session.revoke(reason, now));
    }
    private void revokeSessionEntity(AuthSessionEntity session, String reason, Instant now) {
        session.revoke(reason, now);
        refreshTokens.findActiveByFamilyId(session.getFamilyId()).forEach(token -> token.revoke(reason, now));
    }
    private SessionPayload sessionPayload(UserEntity user, JwtService.AccessToken access, UUID browserSessionId) {
        return new SessionPayload(access.value(), access.expiresAt(), roles.findActiveRoleCodesByUserId(user.getUserId()).stream().sorted().toList(),
                browserSessionId);
    }
    private String sanitizeDeviceLabel(String label) {
        String sanitized = label == null ? "Web browser" : label.replaceAll("[\\r\\n\\t]", " ").trim();
        return sanitized.isBlank() ? "Web browser" : sanitized.substring(0, Math.min(sanitized.length(), 120));
    }

    public record SessionPayload(String accessToken, Instant expiresAt, List<String> roles, UUID browserSessionId) { }
    public record BrowserSession(SessionPayload session, String refreshToken, String csrfToken) { }
    private record RefreshReplay(String accessToken, Instant expiresAt, List<String> roles, UUID browserSessionId,
                                 String refreshToken, String csrfToken) { }
}
