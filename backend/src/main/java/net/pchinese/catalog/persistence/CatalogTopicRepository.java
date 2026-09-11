package net.pchinese.catalog.persistence;

import net.pchinese.catalog.api.dto.TopicSummaryDto;
import net.pchinese.catalog.domain.PublicationState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CatalogTopicRepository extends JpaRepository<CatalogTopicEntity, UUID> {

    @Query("""
        SELECT new net.pchinese.catalog.api.dto.TopicSummaryDto(
            t.id, t.title, t.slug, t.description, t.hskLevel, count(l.id)
        )
        FROM CatalogTopicEntity t
        LEFT JOIN CatalogLessonEntity l ON t.id = l.topicId
            AND l.publicationState = 'PUBLISHED'
            AND l.accessLevel = 'FREE'
        WHERE t.publicationState = :state
          AND (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', cast(:keyword as string), '%')))
          AND (:hskLevel IS NULL OR t.hskLevel = :hskLevel)
        GROUP BY t.id, t.title, t.slug, t.description, t.hskLevel, t.sortOrder
        ORDER BY t.sortOrder ASC
    """)
    Page<TopicSummaryDto> searchPublishedTopics(
        @Param("state") PublicationState state,
        @Param("keyword") String keyword,
        @Param("hskLevel") Short hskLevel,
        Pageable pageable
    );
}
