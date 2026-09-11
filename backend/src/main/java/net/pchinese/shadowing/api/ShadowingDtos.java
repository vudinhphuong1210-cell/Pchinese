package net.pchinese.shadowing.api;

import jakarta.validation.constraints.NotNull;
import net.pchinese.shadowing.domain.RecordingStatus;
import net.pchinese.shadowing.domain.ShadowingAttemptStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class ShadowingDtos {
    private ShadowingDtos() {}

    public record RecordingView(
            UUID recordingId,
            RecordingStatus status,
            String mimeType,
            long byteSize,
            Integer durationMilliseconds,
            Instant expiresAt
    ) {}

    public record CreateAttemptRequest(
            @NotNull UUID recordingId,
            UUID clientRequestId
    ) {}

    public record ShadowingAttemptView(
            UUID attemptId,
            UUID lessonId,
            UUID segmentId,
            UUID recordingId,
            ShadowingAttemptStatus status,
            BigDecimal overallScore,
            BigDecimal pronunciationScore,
            BigDecimal toneScore,
            BigDecimal rhythmScore,
            String feedback,
            Instant submittedAt,
            Instant evaluatedAt
    ) {}
}
