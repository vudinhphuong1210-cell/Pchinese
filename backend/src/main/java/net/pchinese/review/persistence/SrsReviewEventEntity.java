package net.pchinese.review.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import net.pchinese.review.domain.ReviewRating;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "srs_review_events")
public class SrsReviewEventEntity {

    @Id
    @Column(name = "srs_review_event_id", nullable = false)
    private UUID srsReviewEventId;

    @Column(name = "srs_schedule_id", nullable = false)
    private UUID srsScheduleId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "client_review_id", nullable = false)
    private UUID clientReviewId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rating", nullable = false)
    private ReviewRating rating;

    @Column(name = "previous_due_at", nullable = false)
    private Instant previousDueAt;

    @Column(name = "next_due_at", nullable = false)
    private Instant nextDueAt;

    @Column(name = "previous_interval_days", nullable = false)
    private BigDecimal previousIntervalDays;

    @Column(name = "next_interval_days", nullable = false)
    private BigDecimal nextIntervalDays;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SrsReviewEventEntity() {}

    public SrsReviewEventEntity(
            UUID srsReviewEventId,
            UUID srsScheduleId,
            UUID userId,
            UUID clientReviewId,
            ReviewRating rating,
            Instant previousDueAt,
            Instant nextDueAt,
            BigDecimal previousIntervalDays,
            BigDecimal nextIntervalDays,
            Instant reviewedAt,
            Instant createdAt
    ) {
        this.srsReviewEventId = srsReviewEventId;
        this.srsScheduleId = srsScheduleId;
        this.userId = userId;
        this.clientReviewId = clientReviewId;
        this.rating = rating;
        this.previousDueAt = previousDueAt;
        this.nextDueAt = nextDueAt;
        this.previousIntervalDays = previousIntervalDays;
        this.nextIntervalDays = nextIntervalDays;
        this.reviewedAt = reviewedAt;
        this.createdAt = createdAt;
    }

    public UUID getSrsReviewEventId() {
        return srsReviewEventId;
    }

    public void setSrsReviewEventId(UUID srsReviewEventId) {
        this.srsReviewEventId = srsReviewEventId;
    }

    public UUID getSrsScheduleId() {
        return srsScheduleId;
    }

    public void setSrsScheduleId(UUID srsScheduleId) {
        this.srsScheduleId = srsScheduleId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getClientReviewId() {
        return clientReviewId;
    }

    public void setClientReviewId(UUID clientReviewId) {
        this.clientReviewId = clientReviewId;
    }

    public ReviewRating getRating() {
        return rating;
    }

    public void setRating(ReviewRating rating) {
        this.rating = rating;
    }

    public Instant getPreviousDueAt() {
        return previousDueAt;
    }

    public void setPreviousDueAt(Instant previousDueAt) {
        this.previousDueAt = previousDueAt;
    }

    public Instant getNextDueAt() {
        return nextDueAt;
    }

    public void setNextDueAt(Instant nextDueAt) {
        this.nextDueAt = nextDueAt;
    }

    public BigDecimal getPreviousIntervalDays() {
        return previousIntervalDays;
    }

    public void setPreviousIntervalDays(BigDecimal previousIntervalDays) {
        this.previousIntervalDays = previousIntervalDays;
    }

    public BigDecimal getNextIntervalDays() {
        return nextIntervalDays;
    }

    public void setNextIntervalDays(BigDecimal nextIntervalDays) {
        this.nextIntervalDays = nextIntervalDays;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
