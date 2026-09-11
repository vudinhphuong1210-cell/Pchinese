package net.pchinese.review.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import net.pchinese.review.domain.SrsStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "srs_schedules")
public class SrsScheduleEntity {

    @Id
    @Column(name = "srs_schedule_id", nullable = false)
    private UUID srsScheduleId;

    @Column(name = "saved_word_id", nullable = false, unique = true)
    private UUID savedWordId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SrsStatus status;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "interval_days", nullable = false)
    private BigDecimal intervalDays;

    @Column(name = "ease_factor", nullable = false)
    private BigDecimal easeFactor;

    @Column(name = "repetitions", nullable = false)
    private int repetitions;

    @Column(name = "lapses", nullable = false)
    private int lapses;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public SrsScheduleEntity() {}

    public SrsScheduleEntity(
            UUID srsScheduleId,
            UUID savedWordId,
            UUID userId,
            SrsStatus status,
            Instant dueAt,
            BigDecimal intervalDays,
            BigDecimal easeFactor,
            int repetitions,
            int lapses,
            Instant lastReviewedAt,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
        this.srsScheduleId = srsScheduleId;
        this.savedWordId = savedWordId;
        this.userId = userId;
        this.status = status;
        this.dueAt = dueAt;
        this.intervalDays = intervalDays;
        this.easeFactor = easeFactor;
        this.repetitions = repetitions;
        this.lapses = lapses;
        this.lastReviewedAt = lastReviewedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public UUID getSrsScheduleId() {
        return srsScheduleId;
    }

    public void setSrsScheduleId(UUID srsScheduleId) {
        this.srsScheduleId = srsScheduleId;
    }

    public UUID getSavedWordId() {
        return savedWordId;
    }

    public void setSavedWordId(UUID savedWordId) {
        this.savedWordId = savedWordId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public SrsStatus getStatus() {
        return status;
    }

    public void setStatus(SrsStatus status) {
        this.status = status;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public BigDecimal getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(BigDecimal intervalDays) {
        this.intervalDays = intervalDays;
    }

    public BigDecimal getEaseFactor() {
        return easeFactor;
    }

    public void setEaseFactor(BigDecimal easeFactor) {
        this.easeFactor = easeFactor;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public void setRepetitions(int repetitions) {
        this.repetitions = repetitions;
    }

    public int getLapses() {
        return lapses;
    }

    public void setLapses(int lapses) {
        this.lapses = lapses;
    }

    public Instant getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(Instant lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
