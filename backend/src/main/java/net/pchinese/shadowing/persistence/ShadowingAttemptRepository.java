package net.pchinese.shadowing.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShadowingAttemptRepository extends JpaRepository<ShadowingAttemptEntity, UUID> {
    Optional<ShadowingAttemptEntity> findByShadowingAttemptIdAndUserId(UUID attemptId, UUID userId);
    Optional<ShadowingAttemptEntity> findByRecordingId(UUID recordingId);
    Page<ShadowingAttemptEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Page<ShadowingAttemptEntity> findByUserIdAndLessonIdOrderByCreatedAtDesc(UUID userId, UUID lessonId, Pageable pageable);
    Page<ShadowingAttemptEntity> findByUserIdAndLessonIdAndSegmentIdOrderByCreatedAtDesc(UUID userId, UUID lessonId, UUID segmentId, Pageable pageable);
}
