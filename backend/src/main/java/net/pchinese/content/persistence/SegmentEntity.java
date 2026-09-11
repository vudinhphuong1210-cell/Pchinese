package net.pchinese.content.persistence;

import jakarta.persistence.*;
import net.pchinese.content.domain.PublicationState;
import net.pchinese.content.domain.SegmentType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "segments", uniqueConstraints = {
    @UniqueConstraint(name = "uk_segments_lesson_sequence", columnNames = {"lesson_id", "sequence_no"})
})
public class SegmentEntity {

    @Id
    @Column(name = "segment_id", nullable = false)
    private UUID segmentId;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "media_asset_id", nullable = false)
    private UUID mediaAssetId;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "segment_type", nullable = false, length = 50)
    private SegmentType segmentType = SegmentType.BOTH;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_state", nullable = false, length = 50)
    private PublicationState publicationState = PublicationState.DRAFT;

    @Column(name = "start_milliseconds", nullable = false)
    private Integer startMilliseconds = 0;

    @Column(name = "end_milliseconds", nullable = false)
    private Integer endMilliseconds = 0;

    @Column(name = "transcript_hanzi", nullable = false, columnDefinition = "TEXT")
    private String transcriptHanzi;

    @Column(name = "transcript_pinyin", columnDefinition = "TEXT")
    private String transcriptPinyin;

    @Column(name = "translation_vi", columnDefinition = "TEXT")
    private String translationVi;

    @Column(name = "dictation_hint", columnDefinition = "TEXT")
    private String dictationHint;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "updated_by_user_id", nullable = false)
    private UUID updatedByUserId;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public SegmentEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (segmentType == null) {
            segmentType = SegmentType.BOTH;
        }
        if (publicationState == null) {
            publicationState = PublicationState.DRAFT;
        }
        if (startMilliseconds == null) {
            startMilliseconds = 0;
        }
        if (endMilliseconds == null) {
            endMilliseconds = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(UUID segmentId) {
        this.segmentId = segmentId;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public void setLessonId(UUID lessonId) {
        this.lessonId = lessonId;
    }

    public UUID getMediaAssetId() {
        return mediaAssetId;
    }

    public void setMediaAssetId(UUID mediaAssetId) {
        this.mediaAssetId = mediaAssetId;
    }

    public Integer getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Integer sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public SegmentType getSegmentType() {
        return segmentType;
    }

    public void setSegmentType(SegmentType segmentType) {
        this.segmentType = segmentType;
    }

    public PublicationState getPublicationState() {
        return publicationState;
    }

    public void setPublicationState(PublicationState publicationState) {
        this.publicationState = publicationState;
    }

    public Integer getStartMilliseconds() {
        return startMilliseconds;
    }

    public void setStartMilliseconds(Integer startMilliseconds) {
        this.startMilliseconds = startMilliseconds;
    }

    public Integer getEndMilliseconds() {
        return endMilliseconds;
    }

    public void setEndMilliseconds(Integer endMilliseconds) {
        this.endMilliseconds = endMilliseconds;
    }

    public String getTranscriptHanzi() {
        return transcriptHanzi;
    }

    public void setTranscriptHanzi(String transcriptHanzi) {
        this.transcriptHanzi = transcriptHanzi;
    }

    public String getTranscriptPinyin() {
        return transcriptPinyin;
    }

    public void setTranscriptPinyin(String transcriptPinyin) {
        this.transcriptPinyin = transcriptPinyin;
    }

    public String getTranslationVi() {
        return translationVi;
    }

    public void setTranslationVi(String translationVi) {
        this.translationVi = translationVi;
    }

    public String getDictationHint() {
        return dictationHint;
    }

    public void setDictationHint(String dictationHint) {
        this.dictationHint = dictationHint;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public UUID getUpdatedByUserId() {
        return updatedByUserId;
    }

    public void setUpdatedByUserId(UUID updatedByUserId) {
        this.updatedByUserId = updatedByUserId;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
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
