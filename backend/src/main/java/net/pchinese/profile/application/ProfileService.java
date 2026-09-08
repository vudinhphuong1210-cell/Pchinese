package net.pchinese.profile.application;

import net.pchinese.auth.application.AuthAuditService;
import net.pchinese.common.error.ApiException;
import net.pchinese.security.SensitiveValueService;
import net.pchinese.security.UserPrincipal;
import net.pchinese.users.domain.UserStatus;
import net.pchinese.users.persistence.UserEntity;
import net.pchinese.users.persistence.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class ProfileService {
    private static final int MAX_DISPLAY_NAME_LENGTH = 120;
    private static final int MAX_NATIVE_LANGUAGE_CODE_LENGTH = 10;
    private static final int MAX_INTERFACE_LOCALE_LENGTH = 16;
    private static final int MAX_TIME_ZONE_LENGTH = 64;
    private static final int MAX_DAILY_GOAL_MINUTES = 240;

    private final UserRepository users;
    private final SensitiveValueService sensitiveValues;
    private final AuthAuditService audit;

    public ProfileService(UserRepository users, SensitiveValueService sensitiveValues, AuthAuditService audit) {
        this.users = users;
        this.sensitiveValues = sensitiveValues;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public ProfileView getCurrentProfile(UserPrincipal actor) {
        UserEntity user = users.findByUserIdAndStatusAndEmailVerifiedAtIsNotNull(requireActor(actor), UserStatus.ACTIVE)
                .orElseThrow(ApiException::unauthenticated);
        return profileView(user);
    }

    @Transactional
    public ProfileView update(UserPrincipal actor, ProfileUpdateCommand command) {
        if (command == null || command.expectedProfileVersion() == null || command.expectedProfileVersion() < 0) {
            throw ApiException.validation("A valid profile version is required.");
        }
        UserEntity user = users.findLockedById(requireActor(actor)).orElseThrow(ApiException::unauthenticated);
        if (!user.isActiveVerified()) {
            throw ApiException.unauthenticated();
        }
        if (user.getVersion() != command.expectedProfileVersion()) {
            throw ApiException.conflict("Your profile has changed. Reload it before saving again.");
        }

        String displayName = normalized(command.displayName(), MAX_DISPLAY_NAME_LENGTH, "display name");
        String nativeLanguageCode = normalized(command.nativeLanguageCode(), MAX_NATIVE_LANGUAGE_CODE_LENGTH, "native language");
        String interfaceLocale = normalized(command.interfaceLocale(), MAX_INTERFACE_LOCALE_LENGTH, "interface locale");
        String timeZone = normalized(command.timeZone(), MAX_TIME_ZONE_LENGTH, "time zone");
        validateTimeZone(timeZone);
        validateTargetHskLevel(command.targetHskLevel());
        validateDailyGoal(command.dailyGoalMinutes());

        String currentDisplayName = decryptDisplayName(user);
        String nextDisplayName = displayName == null ? currentDisplayName : displayName;
        String nextNativeLanguageCode = nativeLanguageCode == null ? user.getNativeLanguageCode() : nativeLanguageCode;
        String nextInterfaceLocale = interfaceLocale == null ? user.getInterfaceLocale() : interfaceLocale;
        String nextTimeZone = timeZone == null ? user.getTimeZone() : timeZone;
        Integer nextTargetHskLevel = command.targetHskLevel() == null ? user.getTargetHskLevel() : command.targetHskLevel();
        int nextDailyGoalMinutes = command.dailyGoalMinutes() == null ? user.getDailyGoalMinutes() : command.dailyGoalMinutes();

        Set<String> changedFields = changedFields(user, currentDisplayName, nextDisplayName, nextNativeLanguageCode,
                nextInterfaceLocale, nextTimeZone, nextTargetHskLevel, nextDailyGoalMinutes);
        if (changedFields.isEmpty()) {
            return profileView(user);
        }

        byte[] encryptedDisplayName = Objects.equals(currentDisplayName, nextDisplayName)
                ? user.getDisplayNameCiphertext() : sensitiveValues.encrypt(nextDisplayName);
        Instant now = Instant.now();
        user.updateProfile(encryptedDisplayName, nextNativeLanguageCode, nextInterfaceLocale, nextTimeZone,
                nextTargetHskLevel, nextDailyGoalMinutes, now);
        users.flush();
        audit.recordProfilePreferencesUpdated(user.getUserId(), actor.sessionId(), changedFields, now);
        return profileView(user);
    }

    private ProfileView profileView(UserEntity user) {
        return new ProfileView(decryptDisplayName(user), user.getNativeLanguageCode(), user.getInterfaceLocale(), user.getTimeZone(),
                user.getTargetHskLevel(), user.getDailyGoalMinutes(), user.getVersion());
    }

    private Set<String> changedFields(UserEntity user, String currentDisplayName, String nextDisplayName,
                                      String nextNativeLanguageCode, String nextInterfaceLocale, String nextTimeZone,
                                      Integer nextTargetHskLevel, int nextDailyGoalMinutes) {
        Set<String> fields = new LinkedHashSet<>();
        if (!Objects.equals(currentDisplayName, nextDisplayName)) fields.add("DISPLAY_NAME");
        if (!Objects.equals(user.getNativeLanguageCode(), nextNativeLanguageCode)) fields.add("NATIVE_LANGUAGE_CODE");
        if (!Objects.equals(user.getInterfaceLocale(), nextInterfaceLocale)) fields.add("INTERFACE_LOCALE");
        if (!Objects.equals(user.getTimeZone(), nextTimeZone)) fields.add("TIME_ZONE");
        if (!Objects.equals(user.getTargetHskLevel(), nextTargetHskLevel)) fields.add("TARGET_HSK_LEVEL");
        if (user.getDailyGoalMinutes() != nextDailyGoalMinutes) fields.add("DAILY_GOAL_MINUTES");
        return fields;
    }

    private String decryptDisplayName(UserEntity user) {
        return user.getDisplayNameCiphertext() == null ? null : sensitiveValues.decryptToString(user.getDisplayNameCiphertext());
    }

    private UUID requireActor(UserPrincipal actor) {
        if (actor == null || actor.userId() == null) {
            throw ApiException.unauthenticated();
        }
        return actor.userId();
    }

    private String normalized(String value, int maximumLength, String fieldName) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > maximumLength) {
            throw ApiException.validation("The " + fieldName + " is not valid.");
        }
        return normalized;
    }

    private void validateTimeZone(String timeZone) {
        if (timeZone == null) {
            return;
        }
        try {
            ZoneId.of(timeZone);
        } catch (DateTimeException exception) {
            throw ApiException.validation("The time zone is not valid.");
        }
    }

    private void validateTargetHskLevel(Integer targetHskLevel) {
        if (targetHskLevel != null && (targetHskLevel < 1 || targetHskLevel > 6)) {
            throw ApiException.validation("The target HSK level must be between 1 and 6.");
        }
    }

    private void validateDailyGoal(Integer dailyGoalMinutes) {
        if (dailyGoalMinutes != null && (dailyGoalMinutes < 1 || dailyGoalMinutes > MAX_DAILY_GOAL_MINUTES)) {
            throw ApiException.validation("The daily goal is not valid.");
        }
    }

    public record ProfileUpdateCommand(String displayName, String nativeLanguageCode, String interfaceLocale, String timeZone,
                                       Integer targetHskLevel, Integer dailyGoalMinutes, Long expectedProfileVersion) { }

    public record ProfileView(String displayName, String nativeLanguageCode, String interfaceLocale, String timeZone,
                              Integer targetHskLevel, int dailyGoalMinutes, long profileVersion) { }
}
