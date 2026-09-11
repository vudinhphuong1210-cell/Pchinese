package net.pchinese.learning.persistence;

import jakarta.persistence.*;
import net.pchinese.learning.domain.LessonProgressStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lesson_progresses", uniqueConstraints = {
        @UniqueConstraint(name = "uq_lesson_progresses_user_lesson", columnNames = {"user_id", "lesson_id"})
})
public class LessonProgressEntity {

    @Id
    @Column(name = "lesson_progress_id", nullable = false)
    private UUID lessonProgressId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private LessonProgressStatus status = LessonProgressStatus.NOT_STARTED;

    @Column(name = "current_segment_id")
    private UUID currentSegmentId;

    @Column(name = "completed_segment_count", nullable = false)
    private int completedSegmentCount = 0;

    @Column(name = "total_segment_count", nullable = false)
    private int totalSegmentCount = 0;

    @Column(name = "completion_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal completionPercent = BigDecimal.ZERO;

    @Column(name = "dictation_best_score", precision = 5, scale = 2)
    private BigDecimal dictationBestScore;

    @Column(name = "shadowing_best_score", precision = 5, scale = 2)
    private BigDecimal shadowingBestScore;

    @Column(name = "practice_seconds", nullable = false)
    private int practiceSeconds = 0;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public LessonProgressEntity() {
    }

    public static LessonProgressEntity initialize(UUID userId, UUID lessonId, UUID firstSegmentId, int totalSegments) {
        LessonProgressEntity entity = new LessonProgressEntity();
        entity.lessonProgressId = UUID.randomUUID();
        entity.userId = userId;
        entity.lessonId = lessonId;
        entity.status = LessonProgressStatus.NOT_STARTED;
        entity.currentSegmentId = firstSegmentId;
        entity.completedSegmentCount = 0;
        entity.totalSegmentCount = totalSegments;
        entity.completionPercent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        entity.practiceSeconds = 0;
        Instant now = Instant.now();
        entity.lastActivityAt = now;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public void advanceWatermark(int additionalSeconds, Instant now) {
        if (this.status == LessonProgressStatus.NOT_STARTED && additionalSeconds > 0) {
            this.status = LessonProgressStatus.IN_PROGRESS;
            if (this.startedAt == null) {
                this.startedAt = now;
            }
        }
        this.practiceSeconds += Math.max(0, additionalSeconds);
        this.lastActivityAt = now;
        this.updatedAt = now;
    }

    public void completeSegment(UUID nextSegmentId, Instant now) {
        if (this.status == LessonProgressStatus.NOT_STARTED) {
            this.status = LessonProgressStatus.IN_PROGRESS;
            if (this.startedAt == null) {
                this.startedAt = now;
            }
        }
        this.completedSegmentCount++;
        if (this.totalSegmentCount > 0) {
            BigDecimal percent = BigDecimal.valueOf(this.completedSegmentCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(this.totalSegmentCount), 2, RoundingMode.HALF_UP);
            this.completionPercent = percent.min(BigDecimal.valueOf(100));
        }
        this.currentSegmentId = nextSegmentId;
        if (nextSegmentId == null || this.completedSegmentCount >= this.totalSegmentCount) {
            this.status = LessonProgressStatus.COMPLETED;
            this.completionPercent = BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
            this.currentSegmentId = null;
            this.completedAt = now;
        }
        this.lastActivityAt = now;
        this.updatedAt = now;
    }

    public void updateDictationScore(BigDecimal score, Instant now) {
        if (score != null) {
            if (this.dictationBestScore == null || score.compareTo(this.dictationBestScore) > 0) {
                this.dictationBestScore = score;
            }
        }
        this.lastActivityAt = now;
        this.updatedAt = now;
    }

    public void updateShadowingScore(BigDecimal score, Instant now) {
        if (score != null) {
            if (this.shadowingBestScore == null || score.compareTo(this.shadowingBestScore) > 0) {
                this.shadowingBestScore = score;
            }
        }
        this.lastActivityAt = now;
        this.updatedAt = now;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (lastActivityAt == null) lastActivityAt = now;
        if (lessonProgressId == null) lessonProgressId = UUID.randomUUID();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getLessonProgressId() { return lessonProgressId; }
    public UUID getUserId() { return userId; }
    public UUID getLessonId() { return lessonId; }
    public LessonProgressStatus getStatus() { return status; }
    public UUID getCurrentSegmentId() { return currentSegmentId; }
    public int getCompletedSegmentCount() { return completedSegmentCount; }
    public int getTotalSegmentCount() { return totalSegmentCount; }
    public BigDecimal getCompletionPercent() { return completionPercent; }
    public BigDecimal getDictationBestScore() { return dictationBestScore; }
    public BigDecimal getShadowingBestScore() { return shadowingBestScore; }
    public int getPracticeSeconds() { return practiceSeconds; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getLastActivityAt() { return lastActivityAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
