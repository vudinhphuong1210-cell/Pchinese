package net.pchinese.dictation.persistence;

import jakarta.persistence.*;
import net.pchinese.dictation.domain.DictationAttemptStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dictation_attempts")
public class DictationAttemptEntity {

    @Id
    @Column(name = "dictation_attempt_id", nullable = false)
    private UUID dictationAttemptId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "segment_id", nullable = false)
    private UUID segmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private DictationAttemptStatus status = DictationAttemptStatus.IN_PROGRESS;

    @Column(name = "answer_ciphertext")
    private byte[] answerCiphertext;

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "accuracy_percent", precision = 5, scale = 2)
    private BigDecimal accuracyPercent;

    @Column(name = "feedback_ciphertext")
    private byte[] feedbackCiphertext;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

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

    public DictationAttemptEntity() {
    }

    public static DictationAttemptEntity start(UUID userId, UUID lessonId, UUID segmentId) {
        DictationAttemptEntity entity = new DictationAttemptEntity();
        entity.dictationAttemptId = UUID.randomUUID();
        entity.userId = userId;
        entity.lessonId = lessonId;
        entity.segmentId = segmentId;
        entity.status = DictationAttemptStatus.IN_PROGRESS;
        Instant now = Instant.now();
        entity.startedAt = now;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public void evaluate(byte[] encryptedAnswer, BigDecimal score, byte[] encryptedFeedback, Instant now) {
        this.answerCiphertext = encryptedAnswer;
        this.overallScore = score;
        this.accuracyPercent = score;
        this.feedbackCiphertext = encryptedFeedback;
        this.status = DictationAttemptStatus.EVALUATED;
        this.submittedAt = now;
        this.evaluatedAt = now;
        this.updatedAt = now;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (startedAt == null) startedAt = now;
        if (dictationAttemptId == null) dictationAttemptId = UUID.randomUUID();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getDictationAttemptId() { return dictationAttemptId; }
    public UUID getUserId() { return userId; }
    public UUID getLessonId() { return lessonId; }
    public UUID getSegmentId() { return segmentId; }
    public DictationAttemptStatus getStatus() { return status; }
    public byte[] getAnswerCiphertext() { return answerCiphertext; }
    public BigDecimal getOverallScore() { return overallScore; }
    public BigDecimal getAccuracyPercent() { return accuracyPercent; }
    public byte[] getFeedbackCiphertext() { return feedbackCiphertext; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
