package net.pchinese.auth.application;

import net.pchinese.auth.domain.ActionTokenPurpose;
import net.pchinese.auth.persistence.AuthActionTokenEntity;
import net.pchinese.auth.persistence.AuthActionTokenRepository;
import net.pchinese.common.error.ApiException;
import net.pchinese.security.SensitiveValueService;
import net.pchinese.users.persistence.UserEntity;
import net.pchinese.users.persistence.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class AccountLifecycleService {
    private final UserRepository users;
    private final AuthActionTokenRepository actionTokens;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final SensitiveValueService sensitiveValues;
    private final AuthActionTokenDelivery delivery;
    private final AuthAuditService audit;
    private final SessionLifecycleService sessions;
    private final net.pchinese.entitlement.application.EntitlementProvisioningService entitlementProvisioning;

    public AccountLifecycleService(UserRepository users, AuthActionTokenRepository actionTokens, PasswordEncoder passwordEncoder,
                                   PasswordPolicy passwordPolicy, SensitiveValueService sensitiveValues, AuthActionTokenDelivery delivery,
                                   AuthAuditService audit, SessionLifecycleService sessions,
                                   net.pchinese.entitlement.application.EntitlementProvisioningService entitlementProvisioning) {
        this.users = users; this.actionTokens = actionTokens; this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy; this.sensitiveValues = sensitiveValues; this.delivery = delivery;
        this.audit = audit; this.sessions = sessions; this.entitlementProvisioning = entitlementProvisioning;
    }

    @Transactional
    public void register(String email, String password) {
        passwordPolicy.validate(password);
        String normalized = normalizeEmail(email);
        Instant now = Instant.now();
        UserEntity user = users.findByEmailLookupHash(sensitiveValues.hashEmail(normalized)).orElse(null);
        if (user == null) {
            user = UserEntity.pending(sensitiveValues.encrypt(normalized), sensitiveValues.hashEmail(normalized), passwordEncoder.encode(password), now);
            users.save(user);
            issueActionToken(user, ActionTokenPurpose.EMAIL_VERIFICATION, now);
        } else if (!user.isActiveVerified()) {
            issueActionToken(user, ActionTokenPurpose.EMAIL_VERIFICATION, now);
        }
        // Every valid request returns the same neutral accepted result at the controller boundary.
    }

    @Transactional
    public void requestVerification(String email) {
        String normalized = normalizeEmail(email);
        Instant now = Instant.now();
        users.findByEmailLookupHash(sensitiveValues.hashEmail(normalized))
                .filter(user -> !user.isActiveVerified())
                .ifPresent(user -> issueActionToken(user, ActionTokenPurpose.EMAIL_VERIFICATION, now));
    }

    @Transactional
    public void confirmVerification(String rawToken) {
        AuthActionTokenEntity token = usableToken(rawToken, ActionTokenPurpose.EMAIL_VERIFICATION);
        UserEntity user = token.getUser();
        if (user.isActiveVerified()) throw ApiException.conflict("The verification credential cannot be used.");
        Instant now = Instant.now();
        user.activate(now); token.consume(now);
        entitlementProvisioning.ensureFreeEntitlement(user.getUserId());
        audit.record(AuditEventTaxonomy.EventType.EMAIL_VERIFIED, user.getUserId(), user.getUserId(), null, null, null,
                audit.details(AuditEventTaxonomy.OutcomeCode.SUCCESS), now);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        String normalized = normalizeEmail(email);
        Instant now = Instant.now();
        users.findByEmailLookupHash(sensitiveValues.hashEmail(normalized))
                .filter(UserEntity::isActiveVerified)
                .ifPresent(user -> issueActionToken(user, ActionTokenPurpose.PASSWORD_RESET, now));
    }

    @Transactional
    public void confirmPasswordReset(String rawToken, String newPassword) {
        passwordPolicy.validate(newPassword);
        AuthActionTokenEntity token = usableToken(rawToken, ActionTokenPurpose.PASSWORD_RESET);
        Instant now = Instant.now();
        UserEntity user = token.getUser();
        user.replacePassword(passwordEncoder.encode(newPassword), now);
        token.consume(now);
        sessions.revokeAllForUser(user.getUserId(), "PASSWORD_RESET", now);
        audit.record(AuditEventTaxonomy.EventType.PASSWORD_RESET, user.getUserId(), user.getUserId(), null, null, null,
                audit.details(AuditEventTaxonomy.OutcomeCode.SUCCESS), now);
    }

    private AuthActionTokenEntity usableToken(String rawToken, ActionTokenPurpose purpose) {
        AuthActionTokenEntity token = actionTokens.findLockedByTokenHash(sensitiveValues.hashToken(rawToken))
                .orElseThrow(() -> ApiException.validation("The action credential is invalid."));
        if (token.getPurpose() != purpose || !token.isUsable(Instant.now())) {
            throw ApiException.conflict("The action credential cannot be used.");
        }
        return token;
    }
    private void issueActionToken(UserEntity user, ActionTokenPurpose purpose, Instant now) {
        actionTokens.findOpenByUserAndPurpose(user.getUserId(), purpose).forEach(token -> token.invalidate(now));
        String raw = sensitiveValues.randomOpaqueToken();
        actionTokens.save(AuthActionTokenEntity.create(user, purpose, sensitiveValues.hashToken(raw), now));
        delivery.deliver(user.getUserId(), purpose, raw);
    }
    private String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
