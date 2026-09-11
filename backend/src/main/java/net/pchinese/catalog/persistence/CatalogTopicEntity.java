package net.pchinese.catalog.persistence;

import jakarta.persistence.*;
import net.pchinese.catalog.domain.PublicationState;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Table(name = "topics")
@Immutable
public class CatalogTopicEntity {

    @Id
    @Column(name = "topic_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "slug", updatable = false, nullable = false)
    private String slug;

    @Column(name = "title", updatable = false, nullable = false)
    private String title;

    @Column(name = "description", updatable = false)
    private String description;

    @Column(name = "hsk_level", updatable = false)
    private Short hskLevel;

    @Column(name = "sort_order", updatable = false, nullable = false)
    private Integer sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_state", updatable = false, nullable = false)
    private PublicationState publicationState;

    protected CatalogTopicEntity() {
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Short getHskLevel() {
        return hskLevel;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public PublicationState getPublicationState() {
        return publicationState;
    }
}
