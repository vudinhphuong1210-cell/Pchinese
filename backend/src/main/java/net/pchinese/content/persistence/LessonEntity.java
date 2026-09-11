package net.pchinese.content.persistence;

import jakarta.persistence.*;
import net.pchinese.content.domain.AccessLevel;
import net.pchinese.content.domain.LessonType;
import net.pchinese.content.domain.PublicationState;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lessons")
public class LessonEntity {

    @Id
    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    @Column(name = "slug", nullable = false, unique = true, length = 200)
    private String slug;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "hsk_level", columnDefinition = "smallint")
    private Short hskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type", nullable = false, length = 50)
    private LessonType lessonType = LessonType.AUDIO;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 50)
    private AccessLevel accessLevel = AccessLevel.FREE;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_state", nullable = false, length = 50)
    private PublicationState publicationState = PublicationState.DRAFT;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "estimated_duration_seconds", nullable = false)
    private Integer estimatedDurationSeconds = 0;

    @Column(name = "completion_min_percent", nullable = false, columnDefinition = "smallint")
    private Short completionMinPercent = 100;

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

    public LessonEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (lessonType == null) {
            lessonType = LessonType.AUDIO;
        }
        if (accessLevel == null) {
            accessLevel = AccessLevel.FREE;
        }
        if (publicationState == null) {
            publicationState = PublicationState.DRAFT;
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
        if (estimatedDurationSeconds == null) {
            estimatedDurationSeconds = 0;
        }
        if (completionMinPercent == null) {
            completionMinPercent = 100;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public void setLessonId(UUID lessonId) {
        this.lessonId = lessonId;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public void setTopicId(UUID topicId) {
        this.topicId = topicId;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Short getHskLevel() {
        return hskLevel;
    }

    public void setHskLevel(Short hskLevel) {
        this.hskLevel = hskLevel;
    }

    public LessonType getLessonType() {
        return lessonType;
    }

    public void setLessonType(LessonType lessonType) {
        this.lessonType = lessonType;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    public PublicationState getPublicationState() {
        return publicationState;
    }

    public void setPublicationState(PublicationState publicationState) {
        this.publicationState = publicationState;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getEstimatedDurationSeconds() {
        return estimatedDurationSeconds;
    }

    public void setEstimatedDurationSeconds(Integer estimatedDurationSeconds) {
        this.estimatedDurationSeconds = estimatedDurationSeconds;
    }

    public Short getCompletionMinPercent() {
        return completionMinPercent;
    }

    public void setCompletionMinPercent(Short completionMinPercent) {
        this.completionMinPercent = completionMinPercent;
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
