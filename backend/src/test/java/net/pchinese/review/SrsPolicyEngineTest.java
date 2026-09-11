package net.pchinese.review;

import net.pchinese.review.domain.ReviewRating;
import net.pchinese.review.domain.SrsPolicyEngine;
import net.pchinese.review.domain.SrsStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SrsPolicyEngineTest {

    private final Instant now = Instant.parse("2026-09-11T12:00:00Z");
    private final BigDecimal defaultEase = new BigDecimal("2.500");

    @Nested
    @DisplayName("Initial Reviews (Status = LEARNING)")
    class InitialReviews {

        @Test
        @DisplayName("AGAIN rating sets 10-min due time and retains LEARNING status")
        void testInitialAgain() {
            var result = SrsPolicyEngine.calculate(
                    SrsStatus.LEARNING,
                    ReviewRating.AGAIN,
                    BigDecimal.ZERO,
                    defaultEase,
                    0,
                    0,
                    now
            );

            assertThat(result.newStatus()).isEqualTo(SrsStatus.LEARNING);
            assertThat(result.newDueAt()).isEqualTo(now.plusSeconds(600));
            assertThat(result.newIntervalDays()).isEqualTo(new BigDecimal("0.007"));
            assertThat(result.newEaseFactor()).isEqualTo(defaultEase);
            assertThat(result.newRepetitions()).isEqualTo(0);
        }

        @Test
        @DisplayName("HARD rating sets 1 day interval and retains LEARNING status")
        void testInitialHard() {
            var result = SrsPolicyEngine.calculate(
                    SrsStatus.LEARNING,
                    ReviewRating.HARD,
                    BigDecimal.ZERO,
                    defaultEase,
                    0,
                    0,
                    now
            );

            assertThat(result.newStatus()).isEqualTo(SrsStatus.LEARNING);
            assertThat(result.newDueAt()).isEqualTo(now.plusSeconds(86400));
            assertThat(result.newIntervalDays()).isEqualTo(new BigDecimal("1.000"));
            assertThat(result.newEaseFactor()).isEqualTo(defaultEase);
            assertThat(result.newRepetitions()).isEqualTo(1);
        }

        @Test
        @DisplayName("GOOD rating sets 3 days interval and transitions to REVIEW status")
        void testInitialGood() {
            var result = SrsPolicyEngine.calculate(
                    SrsStatus.LEARNING,
                    ReviewRating.GOOD,
                    BigDecimal.ZERO,
                    defaultEase,
                    0,
                    0,
                    now
            );

            assertThat(result.newStatus()).isEqualTo(SrsStatus.REVIEW);
            assertThat(result.newDueAt()).isEqualTo(now.plusSeconds(86400 * 3));
            assertThat(result.newIntervalDays()).isEqualTo(new BigDecimal("3.000"));
            assertThat(result.newEaseFactor()).isEqualTo(defaultEase);
            assertThat(result.newRepetitions()).isEqualTo(1);
        }

        @Test
        @DisplayName("EASY rating sets 7 days interval and transitions to REVIEW status")
        void testInitialEasy() {
            var result = SrsPolicyEngine.calculate(
                    SrsStatus.LEARNING,
                    ReviewRating.EASY,
                    BigDecimal.ZERO,
                    defaultEase,
                    0,
                    0,
                    now
            );

            assertThat(result.newStatus()).isEqualTo(SrsStatus.REVIEW);
            assertThat(result.newDueAt()).isEqualTo(now.plusSeconds(86400 * 7));
            assertThat(result.newIntervalDays()).isEqualTo(new BigDecimal("7.000"));
            assertThat(result.newEaseFactor()).isEqualTo(defaultEase);
            assertThat(result.newRepetitions()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Later Reviews (Status = REVIEW)")
    class LaterReviews {

        @Test
        @DisplayName("AGAIN rating decreases ease by 0.20, increments lapses, and transitions to RELEARNING")
        void testReviewAgain() {
            var result = SrsPolicyEngine.calculate(
                    SrsStatus.REVIEW,
                    ReviewRating.AGAIN,
                    new BigDecimal("3.000"),
                    defaultEase,
                    1,
                    0,
                    now
            );

            assertThat(result.newStatus()).isEqualTo(SrsStatus.RELEARNING);
            assertThat(result.newDueAt()).isEqualTo(now.plusSeconds(600));
            assertThat(result.newIntervalDays()).isEqualTo(new BigDecimal("0.007"));
            assertThat(result.newEaseFactor()).isEqualTo(new BigDecimal("2.300"));
            assertThat(result.newLapses()).isEqualTo(1);
        }

        @Test
        @DisplayName("HARD rating decreases ease by 0.15 and multiplies interval by 1.20")
        void testReviewHard() {
            var result = SrsPolicyEngine.calculate(
                    SrsStatus.REVIEW,
                    ReviewRating.HARD,
                    new BigDecimal("3.000"),
                    defaultEase,
                    1,
                    0,
                    now
            );

            assertThat(result.newStatus()).isEqualTo(SrsStatus.REVIEW);
            assertThat(result.newEaseFactor()).isEqualTo(new BigDecimal("2.350"));
            assertThat(result.newIntervalDays()).isEqualTo(new BigDecimal("3.600"));
        }

        @Test
        @DisplayName("GOOD rating multiplies interval by ease factor")
        void testReviewGood() {
            var result = SrsPolicyEngine.calculate(
                    SrsStatus.REVIEW,
                    ReviewRating.GOOD,
                    new BigDecimal("3.000"),
                    new BigDecimal("2.500"),
                    1,
                    0,
                    now
            );

            assertThat(result.newStatus()).isEqualTo(SrsStatus.REVIEW);
            assertThat(result.newIntervalDays()).isEqualTo(new BigDecimal("7.500"));
        }
    }

    @Nested
    @DisplayName("Relearning Reviews (Status = RELEARNING)")
    class RelearningReviews {

        @Test
        @DisplayName("GOOD rating transitions status back to REVIEW")
        void testRelearningGood() {
            var result = SrsPolicyEngine.calculate(
                    SrsStatus.RELEARNING,
                    ReviewRating.GOOD,
                    new BigDecimal("0.007"),
                    new BigDecimal("2.300"),
                    1,
                    1,
                    now
            );

            assertThat(result.newStatus()).isEqualTo(SrsStatus.REVIEW);
        }
    }

    @Nested
    @DisplayName("Ease Clamping")
    class EaseClamping {

        @Test
        @DisplayName("Ease factor cannot fall below 1.300")
        void testMinEaseClamp() {
            BigDecimal lowEase = new BigDecimal("1.200");
            assertThat(SrsPolicyEngine.clampEase(lowEase)).isEqualTo(new BigDecimal("1.300"));
        }

        @Test
        @DisplayName("Ease factor cannot exceed 2.500")
        void testMaxEaseClamp() {
            BigDecimal highEase = new BigDecimal("2.800");
            assertThat(SrsPolicyEngine.clampEase(highEase)).isEqualTo(new BigDecimal("2.500"));
        }
    }
}
