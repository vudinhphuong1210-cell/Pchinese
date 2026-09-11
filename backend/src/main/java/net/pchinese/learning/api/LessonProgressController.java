package net.pchinese.learning.api;

import jakarta.validation.Valid;
import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.common.error.ApiException;
import net.pchinese.learning.application.LessonProgressQueryService;
import net.pchinese.learning.application.LessonProgressService;
import net.pchinese.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/lesson-progress")
public class LessonProgressController {

    private final LessonProgressQueryService queryService;
    private final LessonProgressService progressService;

    public LessonProgressController(LessonProgressQueryService queryService,
                                    LessonProgressService progressService) {
        this.queryService = queryService;
        this.progressService = progressService;
    }

    @GetMapping
    public ApiEnvelope<PlaybackDtos.ProgressProjection> getProgress(
            @AuthenticationPrincipal UserPrincipal actor,
            @RequestParam UUID lessonId
    ) {
        if (actor == null) {
            throw ApiException.unauthenticated();
        }
        return ApiEnvelope.success(queryService.getProgress(actor.userId(), lessonId));
    }

    @PostMapping("/{lessonId}/playback-events")
    public ApiEnvelope<PlaybackDtos.ProgressProjection> submitPlaybackEvent(
            @AuthenticationPrincipal UserPrincipal actor,
            @PathVariable UUID lessonId,
            @Valid @RequestBody PlaybackDtos.PlaybackEventRequest request
    ) {
        if (actor == null) {
            throw ApiException.unauthenticated();
        }
        return ApiEnvelope.success(progressService.submitPlaybackEvent(actor.userId(), lessonId, request));
    }
}
