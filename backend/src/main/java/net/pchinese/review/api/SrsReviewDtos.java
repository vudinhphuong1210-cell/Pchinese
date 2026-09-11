package net.pchinese.review.api;

import jakarta.validation.constraints.NotNull;
import net.pchinese.review.domain.ReviewRating;
import net.pchinese.review.domain.SrsStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class SrsReviewDtos {

    public record DueReviewCardResponse(
            UUID srsScheduleId,
            UUID savedWordId,
            UUID dictionaryEntryId,
            String simplifiedHanzi,
            String traditionalHanzi,
            String primaryPinyin,
            Integer hskLevel,
            String wordType,
            String senses,
            UUID audioMediaAssetId,
            String personalNotePlaintext,
            SrsStatus status,
            Instant dueAt,
            BigDecimal intervalDays,
            BigDecimal easeFactor,
            Long scheduleVersion
    ) {}

    public record DueQueueResponse(
            List<DueReviewCardResponse> items,
            int totalDueCount
    ) {}

    public record SubmitReviewRequest(
            @NotNull UUID srsScheduleId,
            @NotNull UUID clientReviewId,
            @NotNull ReviewRating rating,
            @NotNull Long expectedScheduleVersion
    ) {}

    public record SubmitReviewResponse(
            UUID srsScheduleId,
            UUID srsReviewEventId,
            UUID clientReviewId,
            ReviewRating rating,
            SrsStatus previousStatus,
            SrsStatus nextStatus,
            Instant previousDueAt,
            Instant nextDueAt,
            BigDecimal previousIntervalDays,
            BigDecimal nextIntervalDays,
            BigDecimal easeFactor,
            Long newScheduleVersion,
            boolean idempotentReplay
    ) {}
}
