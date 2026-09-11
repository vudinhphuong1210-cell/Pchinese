package net.pchinese.shadowing.persistence;

import jakarta.persistence.*;
import net.pchinese.shadowing.domain.ShadowingAttemptStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shadowing_attempts")
public class ShadowingAttemptEntity {

    @Id
    @Column(name = "shadowing_attempt_id", nullable = false)
    private UUID shadowingAttemptId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "segment_id", nullable = false)
    private UUID segmentId;

    @Column(name = "recording_id", nullable = false, unique = true)
    private UUID recordingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ShadowingAttemptStatus status = ShadowingAttemptStatus.IN_PROGRESS;

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "pronunciation_score", precision = 5, scale = 2)
    private BigDecimal pronunciationScore;

    @Column(name = "tone_score", precision = 5, scale = 2)
    private BigDecimal toneScore;

    @Column(name = "rhythm_score", precision = 5, scale = 2)
    private BigDecimal rhythmScore;

    @Column(name = "feedback_ciphertext")
    private byte[] feedbackCiphertext;

    @Column(name = "ai_usage_event_id")
    private UUID aiUsageEventId;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "evaluated_at")
    private Instant evaluatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public ShadowingAttemptEntity() {
    }

    public static ShadowingAttemptEntity create(UUID userId, UUID lessonId, UUID segmentId, UUID recordingId, UUID aiUsageEventId) {
        ShadowingAttemptEntity entity = new ShadowingAttemptEntity();
        entity.shadowingAttemptId = UUID.randomUUID();
        entity.userId = userId;
        entity.lessonId = lessonId;
        entity.segmentId = segmentId;
        entity.recordingId = recordingId;
        entity.aiUsageEventId = aiUsageEventId;
        entity.status = ShadowingAttemptStatus.IN_PROGRESS;
        Instant now = Instant.now();
        entity.submittedAt = now;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public void succeed(BigDecimal overall, BigDecimal pronunciation, BigDecimal tone, BigDecimal rhythm, byte[] feedbackCiphertext, Instant now) {
        this.status = ShadowingAttemptStatus.EVALUATED;
        this.overallScore = overall;
        this.pronunciationScore = pronunciation;
        this.toneScore = tone;
        this.rhythmScore = rhythm;
        this.feedbackCiphertext = feedbackCiphertext;
        this.evaluatedAt = now;
        this.updatedAt = now;
    }

    public void fail(Instant now) {
        this.status = ShadowingAttemptStatus.FAILED;
        this.updatedAt = now;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (shadowingAttemptId == null) shadowingAttemptId = UUID.randomUUID();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getShadowingAttemptId() { return shadowingAttemptId; }
    public UUID getUserId() { return userId; }
    public UUID getLessonId() { return lessonId; }
    public UUID getSegmentId() { return segmentId; }
    public UUID getRecordingId() { return recordingId; }
    public ShadowingAttemptStatus getStatus() { return status; }
    public BigDecimal getOverallScore() { return overallScore; }
    public BigDecimal getPronunciationScore() { return pronunciationScore; }
    public BigDecimal getToneScore() { return toneScore; }
    public BigDecimal getRhythmScore() { return rhythmScore; }
    public byte[] getFeedbackCiphertext() { return feedbackCiphertext; }
    public UUID getAiUsageEventId() { return aiUsageEventId; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
