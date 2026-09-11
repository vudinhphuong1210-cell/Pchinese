package net.pchinese.dictation.persistence;

import net.pchinese.dictation.domain.DictationAttemptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DictationAttemptRepository extends JpaRepository<DictationAttemptEntity, UUID> {
    Optional<DictationAttemptEntity> findByDictationAttemptIdAndUserId(UUID attemptId, UUID userId);
    Optional<DictationAttemptEntity> findFirstByUserIdAndSegmentIdAndStatusOrderByCreatedAtDesc(UUID userId, UUID segmentId, DictationAttemptStatus status);
    Page<DictationAttemptEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Page<DictationAttemptEntity> findByUserIdAndLessonIdOrderByCreatedAtDesc(UUID userId, UUID lessonId, Pageable pageable);
    Page<DictationAttemptEntity> findByUserIdAndLessonIdAndSegmentIdOrderByCreatedAtDesc(UUID userId, UUID lessonId, UUID segmentId, Pageable pageable);
}
