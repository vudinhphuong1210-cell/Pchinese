package net.pchinese.learning.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.pchinese.learning.domain.LessonProgressStatus;
import net.pchinese.learning.domain.PlaybackEventType;
import net.pchinese.learning.domain.SegmentPlaybackState;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlaybackDtos {
    private PlaybackDtos() {}

    public record PlaybackEventRequest(
            @NotNull UUID segmentId,
            @NotNull PlaybackEventType event,
            @NotNull @Min(0) Integer positionMs,
            @NotNull UUID clientEventId,
            @NotBlank String playbackCapability
    ) {}

    public record SegmentPlayback(
            UUID segmentId,
            int sequenceNo,
            SegmentPlaybackState state,
            Map<String, Object> playbackMetadata
    ) {}

    public record ProgressProjection(
            UUID lessonId,
            LessonProgressStatus status,
            UUID currentSegmentId,
            int completedSegmentCount,
            int totalSegmentCount,
            BigDecimal completionPercent,
            int playbackWatermarkMs,
            BigDecimal dictationBestScore,
            BigDecimal shadowingBestScore
    ) {}

    public record PlaybackProjection(
            UUID lessonId,
            String playbackCapability,
            Instant capabilityExpiresAt,
            List<SegmentPlayback> segments,
            ProgressProjection progress
    ) {}
}
