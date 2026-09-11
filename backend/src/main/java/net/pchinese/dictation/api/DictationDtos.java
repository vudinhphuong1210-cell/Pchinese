package net.pchinese.dictation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.pchinese.dictation.domain.DictationAttemptStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class DictationDtos {
    private DictationDtos() {}

    public record StartAttemptRequest(
            @NotNull UUID lessonId,
            @NotNull UUID segmentId,
            UUID clientRequestId
    ) {}

    public record SubmitAttemptRequest(
            @NotBlank @Size(max = 500) String answer,
            UUID clientRequestId
    ) {}

    public record DictationAttemptView(
            UUID attemptId,
            UUID lessonId,
            UUID segmentId,
            DictationAttemptStatus status,
            BigDecimal overallScore,
            BigDecimal accuracyPercent,
            String feedback,
            Instant startedAt,
            Instant submittedAt,
            Instant evaluatedAt
    ) {}
}
