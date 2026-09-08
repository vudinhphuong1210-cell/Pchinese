package net.pchinese.profile.application;

import net.pchinese.auth.application.AuthAuditService;
import net.pchinese.common.error.ApiException;
import net.pchinese.security.SensitiveValueService;
import net.pchinese.security.UserPrincipal;
import net.pchinese.users.persistence.UserEntity;
import net.pchinese.users.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {
    @Mock private UserRepository users;
    @Mock private SensitiveValueService sensitiveValues;
    @Mock private AuthAuditService audit;
    private ProfileService service;

    @BeforeEach
    void setUp() {
        service = new ProfileService(users, sensitiveValues, audit);
    }

    @Test
    void updatesOnlyApprovedPreferencesAndReturnsTheNewProfileView() {
        UserEntity learner = activeLearner();
        UserPrincipal principal = principalFor(learner);
        byte[] encryptedDisplayName = new byte[] {4, 2};
        when(users.findLockedById(learner.getUserId())).thenReturn(Optional.of(learner));
        when(sensitiveValues.encrypt("Nguyen Van A")).thenReturn(encryptedDisplayName);
        when(sensitiveValues.decryptToString(encryptedDisplayName)).thenReturn("Nguyen Van A");

        ProfileService.ProfileView result = service.update(principal, new ProfileService.ProfileUpdateCommand(
                "Nguyen Van A", "en", "en-US", "Asia/Tokyo", 6, 60, learner.getVersion()));

        assertEquals("Nguyen Van A", result.displayName());
        assertEquals("en", result.nativeLanguageCode());
        assertEquals("en-US", result.interfaceLocale());
        assertEquals("Asia/Tokyo", result.timeZone());
        assertEquals(6, result.targetHskLevel());
        assertEquals(60, result.dailyGoalMinutes());
        verify(sensitiveValues).encrypt("Nguyen Van A");
        verify(audit).recordProfilePreferencesUpdated(
                org.mockito.ArgumentMatchers.eq(learner.getUserId()),
                org.mockito.ArgumentMatchers.eq(principal.sessionId()),
                org.mockito.ArgumentMatchers.eq(Set.of("DISPLAY_NAME", "NATIVE_LANGUAGE_CODE", "INTERFACE_LOCALE", "TIME_ZONE", "TARGET_HSK_LEVEL", "DAILY_GOAL_MINUTES")),
                org.mockito.ArgumentMatchers.any(Instant.class));
    }

    @Test
    void rejectsAStaleVersionWithoutOverwritingAnyProfilePreference() {
        UserEntity learner = activeLearner();
        UserPrincipal principal = principalFor(learner);
        byte[] originalName = new byte[] {1};
        learner.updateProfile(originalName, "vi", "vi-VN", "Asia/Ho_Chi_Minh", 3, 30, Instant.now());
        when(users.findLockedById(learner.getUserId())).thenReturn(Optional.of(learner));

        ApiException exception = assertThrows(ApiException.class, () -> service.update(principal,
                new ProfileService.ProfileUpdateCommand("Overwritten", "en", "en-US", "UTC", 4, 45, learner.getVersion() + 1)));

        assertEquals("STATE_CONFLICT", exception.code());
        assertEquals("vi", learner.getNativeLanguageCode());
        assertEquals("vi-VN", learner.getInterfaceLocale());
        assertEquals("Asia/Ho_Chi_Minh", learner.getTimeZone());
        assertEquals(3, learner.getTargetHskLevel());
        assertEquals(30, learner.getDailyGoalMinutes());
        verify(sensitiveValues, never()).encrypt("Overwritten");
        verifyNoInteractions(audit);
    }

    @Test
    void enforcesTheCanonicalHskRangeBeforePersistingAnyChange() {
        UserEntity learner = activeLearner();
        UserPrincipal principal = principalFor(learner);
        when(users.findLockedById(learner.getUserId())).thenReturn(Optional.of(learner));

        ApiException exception = assertThrows(ApiException.class, () -> service.update(principal,
                new ProfileService.ProfileUpdateCommand("Invalid", "vi", "vi-VN", "UTC", 7, 30, learner.getVersion())));

        assertEquals("VALIDATION_ERROR", exception.code());
        assertEquals("vi", learner.getNativeLanguageCode());
        assertEquals(15, learner.getDailyGoalMinutes());
        verify(sensitiveValues, never()).encrypt("Invalid");
        verifyNoInteractions(audit);
    }

    @Test
    void doesNotRecordAnActivityEventForANoOpProfileUpdate() {
        UserEntity learner = activeLearner();
        when(users.findLockedById(learner.getUserId())).thenReturn(Optional.of(learner));

        service.update(principalFor(learner), new ProfileService.ProfileUpdateCommand(
                null, null, null, null, null, null, learner.getVersion()));

        verifyNoInteractions(audit);
    }

    private UserEntity activeLearner() {
        Instant now = Instant.now();
        UserEntity learner = UserEntity.pending(new byte[] {9}, "a".repeat(64), "password-hash", now);
        learner.activate(now);
        return learner;
    }

    private UserPrincipal principalFor(UserEntity learner) {
        return new UserPrincipal(learner.getUserId(), UUID.randomUUID(), learner.getAuthzVersion(), Set.of());
    }
}
