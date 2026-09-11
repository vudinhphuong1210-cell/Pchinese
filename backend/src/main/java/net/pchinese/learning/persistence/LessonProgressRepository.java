package net.pchinese.learning.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgressEntity, UUID> {
    Optional<LessonProgressEntity> findByUserIdAndLessonId(UUID userId, UUID lessonId);
    boolean existsByUserIdAndLessonId(UUID userId, UUID lessonId);
}
