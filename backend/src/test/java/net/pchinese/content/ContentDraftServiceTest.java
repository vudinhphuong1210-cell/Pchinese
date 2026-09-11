package net.pchinese.content;

import net.pchinese.content.api.ContentValidationException;
import net.pchinese.content.api.StateConflictException;
import net.pchinese.content.application.ContentAuditService;
import net.pchinese.content.application.ContentDraftService;
import net.pchinese.content.domain.AccessLevel;
import net.pchinese.content.domain.PublicationState;
import net.pchinese.content.persistence.*;
import net.pchinese.media.domain.ApprovalStatus;
import net.pchinese.media.persistence.MediaAssetEntity;
import net.pchinese.media.persistence.MediaAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentDraftServiceTest {

    @Mock private TopicRepository topicRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private SegmentRepository segmentRepository;
    @Mock private MediaAssetRepository mediaAssetRepository;
    @Mock private ContentAuditService auditService;

    private ContentDraftService draftService;
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        draftService = new ContentDraftService(topicRepository, lessonRepository, segmentRepository, mediaAssetRepository, auditService);
    }

    @Test
    void createTopic_valid_success() {
        when(topicRepository.existsBySlug("hsk1-basics")).thenReturn(false);
        when(topicRepository.save(any(TopicEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        TopicEntity created = draftService.createTopic("HSK1 Basics", "hsk1-basics", "Desc", 1, 1, actorId);

        assertNotNull(created);
        assertEquals("HSK1 Basics", created.getTitle());
        assertEquals(PublicationState.DRAFT, created.getPublicationState());
        verify(topicRepository).save(any());
    }

    @Test
    void createTopic_duplicateSlug_throwsValidationException() {
        when(topicRepository.existsBySlug("hsk1-basics")).thenReturn(true);

        assertThrows(ContentValidationException.class, () ->
                draftService.createTopic("HSK1 Basics", "hsk1-basics", "Desc", 1, 1, actorId));
    }

    @Test
    void updateTopic_staleVersion_throwsStateConflict() {
        UUID topicId = UUID.randomUUID();
        TopicEntity existing = new TopicEntity();
        existing.setTopicId(topicId);
        existing.setVersion(2L);
        existing.setPublicationState(PublicationState.DRAFT);

        when(topicRepository.findById(topicId)).thenReturn(Optional.of(existing));

        assertThrows(StateConflictException.class, () ->
                draftService.updateTopic(topicId, "New Title", "new-slug", "Desc", 1, 1, 1L, actorId));
    }

    @Test
    void createLesson_nonFreeAccessLevel_throwsValidationException() {
        UUID topicId = UUID.randomUUID();
        TopicEntity topic = new TopicEntity();
        topic.setTopicId(topicId);
        topic.setPublicationState(PublicationState.DRAFT);
        when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));

        assertThrows(ContentValidationException.class, () ->
                draftService.createLesson(topicId, "Lesson 1", "lesson-1", "Summary", 1, 1, AccessLevel.PREMIUM, actorId));
    }

    @Test
    void createSegment_unapprovedMedia_throwsStateConflict() {
        UUID lessonId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId(lessonId);
        lesson.setPublicationState(PublicationState.DRAFT);

        MediaAssetEntity media = new MediaAssetEntity();
        media.setMediaAssetId(mediaId);
        media.setApprovalStatus(ApprovalStatus.REJECTED);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(mediaAssetRepository.findById(mediaId)).thenReturn(Optional.of(media));

        assertThrows(StateConflictException.class, () ->
                draftService.createSegment(lessonId, mediaId, 1, null, 0, 100, "汉字", "pinyin", "Vi", "Hint", actorId));
    }
    @Test
    void createSegment_underPublishedLesson_throwsStateConflict() {
        UUID lessonId = UUID.randomUUID();
        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId(lessonId);
        lesson.setPublicationState(PublicationState.PUBLISHED);
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        assertThrows(StateConflictException.class, () ->
                draftService.createSegment(lessonId, UUID.randomUUID(), 1, null, 0, 100, "汉字", "pinyin", "Vi", "Hint", actorId));
        verifyNoInteractions(mediaAssetRepository);
    }
}
