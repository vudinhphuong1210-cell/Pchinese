package net.pchinese.catalog.application;

import net.pchinese.catalog.api.dto.LessonSummaryDto;
import net.pchinese.catalog.domain.AccessLevel;
import net.pchinese.catalog.domain.PublicationState;
import net.pchinese.catalog.persistence.CatalogLessonEntity;
import net.pchinese.catalog.persistence.CatalogLessonRepository;
import net.pchinese.catalog.persistence.CatalogTopicEntity;
import net.pchinese.catalog.persistence.CatalogTopicRepository;
import net.pchinese.common.error.ApiException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LessonAccessService {

    private final CatalogLessonRepository lessonRepository;
    private final CatalogTopicRepository topicRepository;

    public LessonAccessService(CatalogLessonRepository lessonRepository, CatalogTopicRepository topicRepository) {
        this.lessonRepository = lessonRepository;
        this.topicRepository = topicRepository;
    }

    public LessonSummaryDto getLessonDetail(UUID lessonId) {
        CatalogLessonEntity lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> ApiException.notFound());

        if (lesson.getPublicationState() != PublicationState.PUBLISHED || lesson.getAccessLevel() != AccessLevel.FREE) {
            throw ApiException.notFound();
        }

        CatalogTopicEntity topic = topicRepository.findById(lesson.getTopicId())
                .orElseThrow(() -> ApiException.notFound());

        if (topic.getPublicationState() != PublicationState.PUBLISHED) {
            throw ApiException.notFound();
        }

        // To keep things simple, we don't query segments count again here if not strictly needed,
        // but DTO requires it. Since we fetch one lesson, it's fine to pass 0 or execute a quick query.
        // Wait, we can reuse the repository method if we want, or just return basic summary.
        // Let's get the proper DTO from the repository by passing topic ID.
        return lessonRepository.findPublishedLessonsByTopicId(
                topic.getId(), PublicationState.PUBLISHED, AccessLevel.FREE
        ).stream()
         .filter(l -> l.id().equals(lessonId))
         .findFirst()
         .orElseThrow(() -> ApiException.notFound());
    }
}
