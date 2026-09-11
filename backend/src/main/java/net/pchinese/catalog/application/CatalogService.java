package net.pchinese.catalog.application;

import net.pchinese.catalog.api.dto.LessonSummaryDto;
import net.pchinese.catalog.api.dto.TopicDetailDto;
import net.pchinese.catalog.api.dto.TopicSummaryDto;
import net.pchinese.catalog.domain.AccessLevel;
import net.pchinese.catalog.domain.PublicationState;
import net.pchinese.catalog.persistence.CatalogLessonRepository;
import net.pchinese.catalog.persistence.CatalogTopicEntity;
import net.pchinese.catalog.persistence.CatalogTopicRepository;
import net.pchinese.common.error.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CatalogService {

    private final CatalogTopicRepository topicRepository;
    private final CatalogLessonRepository lessonRepository;

    public CatalogService(CatalogTopicRepository topicRepository, CatalogLessonRepository lessonRepository) {
        this.topicRepository = topicRepository;
        this.lessonRepository = lessonRepository;
    }

    public Page<TopicSummaryDto> searchTopics(String keyword, Short hskLevel, int page, int size) {
        if (size > 50) {
            size = 50;
        }
        return topicRepository.searchPublishedTopics(
                PublicationState.PUBLISHED,
                keyword != null && !keyword.isBlank() ? keyword : null,
                hskLevel,
                PageRequest.of(page, size)
        );
    }

    public Page<LessonSummaryDto> searchLessons(UUID topicId, String keyword, Short hskLevel, int page, int size) {
        if (size > 50) {
            size = 50;
        }
        return lessonRepository.searchPublishedLessons(
                PublicationState.PUBLISHED,
                AccessLevel.FREE,
                topicId,
                keyword != null && !keyword.isBlank() ? keyword : null,
                hskLevel,
                PageRequest.of(page, size)
        );
    }

    public TopicDetailDto getTopicDetail(UUID topicId) {
        CatalogTopicEntity topic = topicRepository.findById(topicId)
                .orElseThrow(() -> ApiException.notFound());
        
        if (topic.getPublicationState() != PublicationState.PUBLISHED) {
            throw ApiException.notFound();
        }

        List<LessonSummaryDto> lessons = lessonRepository.findPublishedLessonsByTopicId(
                topicId,
                PublicationState.PUBLISHED,
                AccessLevel.FREE
        );

        return new TopicDetailDto(
                topic.getId(),
                topic.getTitle(),
                topic.getSlug(),
                topic.getDescription(),
                topic.getHskLevel(),
                lessons
        );
    }
}
