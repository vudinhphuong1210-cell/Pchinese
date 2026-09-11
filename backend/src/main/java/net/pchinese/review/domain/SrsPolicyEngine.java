package net.pchinese.review.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class SrsPolicyEngine {

    public static final BigDecimal MIN_EASE = new BigDecimal("1.300");
    public static final BigDecimal MAX_EASE = new BigDecimal("2.500");

    public record CalculationResult(
            SrsStatus newStatus,
            Instant newDueAt,
            BigDecimal newIntervalDays,
            BigDecimal newEaseFactor,
            int newRepetitions,
            int newLapses
    ) {}

    public static CalculationResult calculate(
            SrsStatus currentStatus,
            ReviewRating rating,
            BigDecimal currentIntervalDays,
            BigDecimal currentEaseFactor,
            int currentRepetitions,
            int currentLapses,
            Instant now
    ) {
        BigDecimal clampedEase = clampEase(currentEaseFactor);

        if (currentStatus == SrsStatus.LEARNING) {
            return calculateLearning(rating, clampedEase, currentLapses, now);
        } else if (currentStatus == SrsStatus.REVIEW) {
            return calculateReview(rating, currentIntervalDays, clampedEase, currentRepetitions, currentLapses, now);
        } else if (currentStatus == SrsStatus.RELEARNING) {
            return calculateRelearning(rating, currentIntervalDays, clampedEase, currentRepetitions, currentLapses, now);
        } else {
            // Default fallback if suspended
            return new CalculationResult(currentStatus, now, currentIntervalDays, clampedEase, currentRepetitions, currentLapses);
        }
    }

    private static CalculationResult calculateLearning(
            ReviewRating rating,
            BigDecimal ease,
            int lapses,
            Instant now
    ) {
        return switch (rating) {
            case AGAIN -> new CalculationResult(
                    SrsStatus.LEARNING,
                    now.plusSeconds(600),
                    new BigDecimal("0.007"),
                    ease,
                    0,
                    lapses
            );
            case HARD -> new CalculationResult(
                    SrsStatus.LEARNING,
                    now.plus(1, ChronoUnit.DAYS),
                    new BigDecimal("1.000"),
                    ease,
                    1,
                    lapses
            );
            case GOOD -> new CalculationResult(
                    SrsStatus.REVIEW,
                    now.plus(3, ChronoUnit.DAYS),
                    new BigDecimal("3.000"),
                    ease,
                    1,
                    lapses
            );
            case EASY -> new CalculationResult(
                    SrsStatus.REVIEW,
                    now.plus(7, ChronoUnit.DAYS),
                    new BigDecimal("7.000"),
                    ease,
                    1,
                    lapses
            );
        };
    }

    private static CalculationResult calculateReview(
            ReviewRating rating,
            BigDecimal prevInterval,
            BigDecimal currentEase,
            int repetitions,
            int lapses,
            Instant now
    ) {
        return switch (rating) {
            case AGAIN -> {
                BigDecimal newEase = clampEase(currentEase.subtract(new BigDecimal("0.20")));
                yield new CalculationResult(
                        SrsStatus.RELEARNING,
                        now.plusSeconds(600),
                        new BigDecimal("0.007"),
                        newEase,
                        repetitions,
                        lapses + 1
                );
            }
            case HARD -> {
                BigDecimal newEase = clampEase(currentEase.subtract(new BigDecimal("0.15")));
                BigDecimal newInterval = prevInterval.multiply(new BigDecimal("1.20")).setScale(3, RoundingMode.HALF_UP);
                Instant newDue = addDaysToInstant(now, newInterval);
                yield new CalculationResult(
                        SrsStatus.REVIEW,
                        newDue,
                        newInterval,
                        newEase,
                        repetitions,
                        lapses
                );
            }
            case GOOD -> {
                BigDecimal newInterval = prevInterval.multiply(currentEase).setScale(3, RoundingMode.HALF_UP);
                Instant newDue = addDaysToInstant(now, newInterval);
                yield new CalculationResult(
                        SrsStatus.REVIEW,
                        newDue,
                        newInterval,
                        currentEase,
                        repetitions + 1,
                        lapses
                );
            }
            case EASY -> {
                BigDecimal multiplier = currentEase.add(new BigDecimal("0.15"));
                BigDecimal newInterval = prevInterval.multiply(multiplier).setScale(3, RoundingMode.HALF_UP);
                Instant newDue = addDaysToInstant(now, newInterval);
                yield new CalculationResult(
                        SrsStatus.REVIEW,
                        newDue,
                        newInterval,
                        currentEase,
                        repetitions + 1,
                        lapses
                );
            }
        };
    }

    private static CalculationResult calculateRelearning(
            ReviewRating rating,
            BigDecimal prevInterval,
            BigDecimal currentEase,
            int repetitions,
            int lapses,
            Instant now
    ) {
        return switch (rating) {
            case AGAIN -> new CalculationResult(
                    SrsStatus.RELEARNING,
                    now.plusSeconds(600),
                    new BigDecimal("0.007"),
                    currentEase,
                    repetitions,
                    lapses
            );
            case HARD -> {
                BigDecimal newInterval = prevInterval.multiply(new BigDecimal("1.20")).setScale(3, RoundingMode.HALF_UP);
                Instant newDue = addDaysToInstant(now, newInterval);
                yield new CalculationResult(
                        SrsStatus.RELEARNING,
                        newDue,
                        newInterval,
                        currentEase,
                        repetitions,
                        lapses
                );
            }
            case GOOD -> {
                BigDecimal newInterval = prevInterval.multiply(currentEase).setScale(3, RoundingMode.HALF_UP);
                Instant newDue = addDaysToInstant(now, newInterval);
                yield new CalculationResult(
                        SrsStatus.REVIEW,
                        newDue,
                        newInterval,
                        currentEase,
                        repetitions + 1,
                        lapses
                );
            }
            case EASY -> {
                BigDecimal multiplier = currentEase.add(new BigDecimal("0.15"));
                BigDecimal newInterval = prevInterval.multiply(multiplier).setScale(3, RoundingMode.HALF_UP);
                Instant newDue = addDaysToInstant(now, newInterval);
                yield new CalculationResult(
                        SrsStatus.REVIEW,
                        newDue,
                        newInterval,
                        currentEase,
                        repetitions + 1,
                        lapses
                );
            }
        };
    }

    public static BigDecimal clampEase(BigDecimal ease) {
        if (ease == null) {
            return MAX_EASE;
        }
        BigDecimal scaled = ease.setScale(3, RoundingMode.HALF_UP);
        if (scaled.compareTo(MIN_EASE) < 0) {
            return MIN_EASE;
        }
        if (scaled.compareTo(MAX_EASE) > 0) {
            return MAX_EASE;
        }
        return scaled;
    }

    private static Instant addDaysToInstant(Instant base, BigDecimal days) {
        long seconds = days.multiply(BigDecimal.valueOf(86400)).longValue();
        return base.plusSeconds(seconds);
    }
}
