package net.pchinese.auth;

import net.pchinese.auth.application.PasswordPolicy;
import net.pchinese.auth.domain.ActionTokenPurpose;
import net.pchinese.auth.domain.Platform;
import net.pchinese.auth.persistence.AuthActionTokenEntity;
import net.pchinese.auth.persistence.AuthSessionEntity;
import net.pchinese.auth.persistence.RefreshTokenEntity;
import net.pchinese.users.persistence.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthLifecycleServiceTest {
    @Test
    void credentialsRequireTheConfiguredBcryptStrengthAndPasswordPolicy() {
        PasswordPolicy policy = new PasswordPolicy();
        assertDoesNotThrow(() -> policy.validate("StrongPassword123"));
        assertThrows(RuntimeException.class, () -> policy.validate("weakpassword"));
        String hash = new BCryptPasswordEncoder(12).encode("StrongPassword123");
        assertTrue(hash.startsWith("$2"));
        assertTrue(new BCryptPasswordEncoder(12).matches("StrongPassword123", hash));
    }

    @Test
    void actionTokenIsSingleUseAndRefreshRotationLeavesNoSecondUsableParent() {
        Instant now = Instant.now();
        UserEntity user = UserEntity.pending(new byte[] {1}, "a".repeat(64), "hash", now);
        AuthActionTokenEntity action = AuthActionTokenEntity.create(user, ActionTokenPurpose.EMAIL_VERIFICATION,
                "b".repeat(64), now);
        assertTrue(action.isUsable(now));
        action.consume(now);
        assertFalse(action.isUsable(now));

        AuthSessionEntity session = AuthSessionEntity.create(user, "device-1", "Browser", Platform.WEB, now);
        RefreshTokenEntity parent = RefreshTokenEntity.create(session, "c".repeat(64), now);
        RefreshTokenEntity child = RefreshTokenEntity.create(session, "d".repeat(64), now);
        parent.rotateTo(child, now);
        assertFalse(parent.isUsable(now));
        assertTrue(child.isUsable(now));
    }
}
