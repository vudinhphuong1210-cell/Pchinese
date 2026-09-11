package net.pchinese.catalog.persistence;

import jakarta.persistence.*;
import net.pchinese.catalog.domain.AccessLevel;
import net.pchinese.catalog.domain.PublicationState;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Table(name = "lessons")
@Immutable
public class CatalogLessonEntity {

    @Id
    @Column(name = "lesson_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "topic_id", updatable = false, nullable = false)
    private UUID topicId;

    @Column(name = "slug", updatable = false, nullable = false)
    private String slug;

    @Column(name = "title", updatable = false, nullable = false)
    private String title;

    @Column(name = "summary", updatable = false)
    private String summary;

    @Column(name = "hsk_level", updatable = false)
    private Short hskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", updatable = false, nullable = false)
    private AccessLevel accessLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_state", updatable = false, nullable = false)
    private PublicationState publicationState;

    @Column(name = "sort_order", updatable = false, nullable = false)
    private Integer sortOrder;

    @Column(name = "estimated_duration_seconds", updatable = false, nullable = false)
    private Integer estimatedDurationSeconds;

    protected CatalogLessonEntity() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public Short getHskLevel() {
        return hskLevel;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public PublicationState getPublicationState() {
        return publicationState;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public Integer getEstimatedDurationSeconds() {
        return estimatedDurationSeconds;
    }
}
