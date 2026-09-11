package net.pchinese.catalog.persistence;

import net.pchinese.catalog.api.dto.LessonSummaryDto;
import net.pchinese.catalog.domain.AccessLevel;
import net.pchinese.catalog.domain.PublicationState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CatalogLessonRepository extends JpaRepository<CatalogLessonEntity, UUID> {

    @Query("""
        SELECT new net.pchinese.catalog.api.dto.LessonSummaryDto(
            l.id, l.topicId, l.title, l.slug, l.summary, l.hskLevel, l.accessLevel, l.estimatedDurationSeconds, count(s.id)
        )
        FROM CatalogLessonEntity l
        LEFT JOIN CatalogSegmentEntity s ON l.id = s.lessonId
            AND s.publicationState = 'PUBLISHED'
        WHERE l.publicationState = :state
          AND l.accessLevel = :access
          AND (:topicId IS NULL OR l.topicId = :topicId)
          AND (:keyword IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%', cast(:keyword as string), '%')))
          AND (:hskLevel IS NULL OR l.hskLevel = :hskLevel)
        GROUP BY l.id, l.topicId, l.title, l.slug, l.summary, l.hskLevel, l.accessLevel, l.estimatedDurationSeconds, l.sortOrder
        ORDER BY l.sortOrder ASC
    """)
    Page<LessonSummaryDto> searchPublishedLessons(
        @Param("state") PublicationState state,
        @Param("access") AccessLevel access,
        @Param("topicId") UUID topicId,
        @Param("keyword") String keyword,
        @Param("hskLevel") Short hskLevel,
        Pageable pageable
    );
    
    @Query("""
        SELECT new net.pchinese.catalog.api.dto.LessonSummaryDto(
            l.id, l.topicId, l.title, l.slug, l.summary, l.hskLevel, l.accessLevel, l.estimatedDurationSeconds, count(s.id)
        )
        FROM CatalogLessonEntity l
        LEFT JOIN CatalogSegmentEntity s ON l.id = s.lessonId
            AND s.publicationState = 'PUBLISHED'
        WHERE l.topicId = :topicId
          AND l.publicationState = :state
          AND l.accessLevel = :access
        GROUP BY l.id, l.topicId, l.title, l.slug, l.summary, l.hskLevel, l.accessLevel, l.estimatedDurationSeconds, l.sortOrder
        ORDER BY l.sortOrder ASC
    """)
    List<LessonSummaryDto> findPublishedLessonsByTopicId(
        @Param("topicId") UUID topicId,
        @Param("state") PublicationState state,
        @Param("access") AccessLevel access
    );
}
