package net.pchinese.catalog.persistence;

import jakarta.persistence.*;
import net.pchinese.catalog.domain.PublicationState;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Table(name = "segments")
@Immutable
public class CatalogSegmentEntity {

    @Id
    @Column(name = "segment_id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "lesson_id", updatable = false, nullable = false)
    private UUID lessonId;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_state", updatable = false, nullable = false)
    private PublicationState publicationState;

    protected CatalogSegmentEntity() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getLessonId() {
        return lessonId;
    }

    public PublicationState getPublicationState() {
        return publicationState;
    }
}
