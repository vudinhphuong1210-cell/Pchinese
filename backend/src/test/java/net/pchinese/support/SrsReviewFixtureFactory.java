package net.pchinese.support;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class SrsReviewFixtureFactory {

    public static final UUID USER_ID_1 = UUID.fromString("00000000-0000-4000-b000-000000000001");
    public static final UUID USER_ID_2 = UUID.fromString("00000000-0000-4000-b000-000000000002");

    public static final UUID SAVED_WORD_ID_1 = UUID.fromString("00000000-0000-4000-c000-000000000001");
    public static final UUID SAVED_WORD_ID_2 = UUID.fromString("00000000-0000-4000-c000-000000000002");

    public static final UUID SRS_SCHEDULE_ID_1 = UUID.fromString("00000000-0000-4000-d000-000000000001");
    public static final UUID SRS_SCHEDULE_ID_2 = UUID.fromString("00000000-0000-4000-d000-000000000002");

    public static final UUID CLIENT_REVIEW_ID_1 = UUID.fromString("00000000-0000-4000-e000-000000000001");

    public record FixtureSrsSchedule(
            UUID srsScheduleId,
            UUID savedWordId,
            UUID userId,
            String status,
            Instant dueAt,
            BigDecimal intervalDays,
            BigDecimal easeFactor,
            Integer repetitions,
            Integer lapses,
            Instant lastReviewedAt,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {}

    public record FixtureSrsReviewEvent(
            UUID srsReviewEventId,
            UUID srsScheduleId,
            UUID userId,
            UUID clientReviewId,
            String rating,
            Instant previousDueAt,
            Instant nextDueAt,
            BigDecimal previousIntervalDays,
            BigDecimal nextIntervalDays,
            Instant reviewedAt,
            Instant createdAt
    ) {}

    public static FixtureSrsSchedule createLearningSchedule(UUID userId, UUID savedWordId, Instant dueAt) {
        return new FixtureSrsSchedule(
                UUID.randomUUID(),
                savedWordId,
                userId,
                "LEARNING",
                dueAt,
                BigDecimal.ZERO,
                new BigDecimal("2.500"),
                0,
                0,
                null,
                dueAt,
                dueAt,
                0L
        );
    }

    public static FixtureSrsSchedule createReviewSchedule(UUID userId, UUID savedWordId, Instant dueAt, BigDecimal intervalDays, BigDecimal easeFactor) {
        return new FixtureSrsSchedule(
                UUID.randomUUID(),
                savedWordId,
                userId,
                "REVIEW",
                dueAt,
                intervalDays,
                easeFactor,
                1,
                0,
                dueAt.minusSeconds(86400),
                dueAt.minusSeconds(86400),
                dueAt.minusSeconds(86400),
                1L
        );
    }

    public static FixtureSrsSchedule createSuspendedSchedule(UUID userId, UUID savedWordId, Instant dueAt) {
        return new FixtureSrsSchedule(
                UUID.randomUUID(),
                savedWordId,
                userId,
                "SUSPENDED",
                dueAt,
                new BigDecimal("3.000"),
                new BigDecimal("2.500"),
                1,
                0,
                dueAt.minusSeconds(86400 * 3),
                dueAt.minusSeconds(86400 * 3),
                dueAt.minusSeconds(86400 * 3),
                1L
        );
    }

    public static FixtureSrsReviewEvent createReviewEvent(UUID scheduleId, UUID userId, UUID clientReviewId, String rating, Instant prevDue, Instant nextDue) {
        return new FixtureSrsReviewEvent(
                UUID.randomUUID(),
                scheduleId,
                userId,
                clientReviewId,
                rating,
                prevDue,
                nextDue,
                BigDecimal.ZERO,
                new BigDecimal("3.000"),
                Instant.now(),
                Instant.now()
        );
    }
}
