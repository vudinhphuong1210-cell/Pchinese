package net.pchinese.progress.api;

import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.common.error.ApiException;
import net.pchinese.progress.application.DailyStreakService;
import net.pchinese.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/daily-streak")
public class DailyStreakController {
    private final DailyStreakService service;

    public DailyStreakController(DailyStreakService service) {
        this.service = service;
    }

    @GetMapping
    public ApiEnvelope<DailyStreakService.DailyStreakView> get(
            @AuthenticationPrincipal UserPrincipal actor) {
        return ApiEnvelope.success(service.get(requireActor(actor).userId()));
    }

    @PostMapping("/check-ins")
    public ApiEnvelope<DailyStreakService.DailyStreakView> checkIn(
            @AuthenticationPrincipal UserPrincipal actor) {
        return ApiEnvelope.success(service.checkIn(requireActor(actor).userId()));
    }

    private static UserPrincipal requireActor(UserPrincipal actor) {
        if (actor == null) throw ApiException.unauthenticated();
        return actor;
    }
}
