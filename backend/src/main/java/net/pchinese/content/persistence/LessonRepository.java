package net.pchinese.content.persistence;

import net.pchinese.content.domain.PublicationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<LessonEntity, UUID> {
    Optional<LessonEntity> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<LessonEntity> findByTopicId(UUID topicId);
    List<LessonEntity> findByTopicIdAndPublicationState(UUID topicId, PublicationState publicationState);
}
