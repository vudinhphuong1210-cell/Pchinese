package net.pchinese.learning.api;

import net.pchinese.common.api.ApiEnvelope;
import net.pchinese.common.error.ApiException;
import net.pchinese.learning.application.LessonPlaybackService;
import net.pchinese.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lessons")
public class LessonPlaybackController {

    private final LessonPlaybackService playbackService;

    public LessonPlaybackController(LessonPlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    @GetMapping("/{lessonId}/playback")
    public ApiEnvelope<PlaybackDtos.PlaybackProjection> getPlayback(
            @AuthenticationPrincipal UserPrincipal actor,
            @PathVariable UUID lessonId
    ) {
        if (actor == null) {
            throw ApiException.unauthenticated();
        }
        return ApiEnvelope.success(playbackService.getPlayback(actor.userId(), lessonId));
    }
}
