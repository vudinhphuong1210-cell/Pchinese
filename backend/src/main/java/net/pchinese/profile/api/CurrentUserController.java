package net.pchinese.profile.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.entitlement.application.EntitlementService;
import net.pchinese.profile.application.ProfileService;
import net.pchinese.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/me")
public class CurrentUserController {
    private final ProfileService profiles;
    private final EntitlementService entitlementService;

    public CurrentUserController(ProfileService profiles, EntitlementService entitlementService) {
        this.profiles = profiles;
        this.entitlementService = entitlementService;
    }

    @GetMapping
    public ApiEnvelope<CurrentUserProjection> getCurrentUser(@AuthenticationPrincipal UserPrincipal actor) {
        ProfileService.ProfileView profile = profiles.getCurrentProfile(actor);
        EntitlementService.EntitlementSummary entitlement = entitlementService.getSafeEntitlementSummary(actor.userId());
        return ApiEnvelope.success(CurrentUserProjection.from(profile, entitlement));
    }

    @PatchMapping
    public ApiEnvelope<CurrentUserProjection> updateCurrentUser(@AuthenticationPrincipal UserPrincipal actor,
                                                                 @Valid @RequestBody ProfileUpdateRequest request) {
        ProfileService.ProfileView profile = profiles.update(actor, request.toCommand());
        EntitlementService.EntitlementSummary entitlement = entitlementService.getSafeEntitlementSummary(actor.userId());
        return ApiEnvelope.success(CurrentUserProjection.from(profile, entitlement));
    }

    public record ProfileUpdateRequest(
            @Size(min = 1, max = 120) String displayName,
            @Size(min = 2, max = 10) String nativeLanguageCode,
            @Size(min = 2, max = 16) String interfaceLocale,
            @Size(min = 1, max = 64) String timeZone,
            @Min(1) @Max(6) Integer targetHskLevel,
            @Min(1) @Max(240) Integer dailyGoalMinutes,
            @NotNull @PositiveOrZero Long expectedProfileVersion) {
        @JsonAnySetter
        public void rejectUnknownField(String name, Object value) {
            throw new IllegalArgumentException("Unsupported profile preference.");
        }

        ProfileService.ProfileUpdateCommand toCommand() {
            return new ProfileService.ProfileUpdateCommand(displayName, nativeLanguageCode, interfaceLocale, timeZone,
                    targetHskLevel, dailyGoalMinutes, expectedProfileVersion);
        }
    }

    public record CurrentUserProjection(Profile profile, EntitlementService.EntitlementSummary entitlement) {
        static CurrentUserProjection from(ProfileService.ProfileView profile, EntitlementService.EntitlementSummary entitlement) {
            return new CurrentUserProjection(Profile.from(profile), entitlement);
        }
    }

    public record Profile(String displayName, String nativeLanguageCode, String interfaceLocale, String timeZone,
                          Integer targetHskLevel, int dailyGoalMinutes, long profileVersion) {
        static Profile from(ProfileService.ProfileView profile) {
            return new Profile(profile.displayName(), profile.nativeLanguageCode(), profile.interfaceLocale(), profile.timeZone(),
                    profile.targetHskLevel(), profile.dailyGoalMinutes(), profile.profileVersion());
        }
    }
}
