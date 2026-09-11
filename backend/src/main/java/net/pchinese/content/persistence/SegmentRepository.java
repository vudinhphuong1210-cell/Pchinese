package net.pchinese.content.persistence;

import net.pchinese.content.domain.PublicationState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SegmentRepository extends JpaRepository<SegmentEntity, UUID> {
    List<SegmentEntity> findByLessonIdOrderBySequenceNoAsc(UUID lessonId);
    List<SegmentEntity> findByLessonIdAndPublicationStateOrderBySequenceNoAsc(UUID lessonId, PublicationState publicationState);
    Optional<SegmentEntity> findByLessonIdAndSequenceNo(UUID lessonId, Integer sequenceNo);
    List<SegmentEntity> findByMediaAssetId(UUID mediaAssetId);
    List<SegmentEntity> findByMediaAssetIdAndPublicationState(UUID mediaAssetId, PublicationState publicationState);
}
