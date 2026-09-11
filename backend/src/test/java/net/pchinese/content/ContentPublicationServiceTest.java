package net.pchinese.content;

import net.pchinese.content.api.StateConflictException;
import net.pchinese.content.application.ContentAuditService;
import net.pchinese.content.application.ContentPublicationService;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentPublicationServiceTest {

    @Mock private TopicRepository topicRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private SegmentRepository segmentRepository;
    @Mock private MediaAssetRepository mediaAssetRepository;
    @Mock private ContentAuditService auditService;

    private ContentPublicationService publicationService;
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        publicationService = new ContentPublicationService(topicRepository, lessonRepository, segmentRepository, mediaAssetRepository, auditService);
    }

    @Test
    void publishLesson_withNoSegments_throwsStateConflict() {
        UUID lessonId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();

        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId(lessonId);
        lesson.setTopicId(topicId);
        lesson.setAccessLevel(AccessLevel.FREE);
        lesson.setPublicationState(PublicationState.DRAFT);
        lesson.setVersion(0L);

        TopicEntity topic = new TopicEntity();
        topic.setTopicId(topicId);
        topic.setPublicationState(PublicationState.PUBLISHED);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));
        when(segmentRepository.findByLessonIdOrderBySequenceNoAsc(lessonId)).thenReturn(List.of());

        assertThrows(StateConflictException.class, () ->
                publicationService.publishLesson(lessonId, 0L, actorId));
    }

    @Test
    void publishLesson_validPrerequisites_success() {
        UUID lessonId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();

        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId(lessonId);
        lesson.setTopicId(topicId);
        lesson.setAccessLevel(AccessLevel.FREE);
        lesson.setPublicationState(PublicationState.DRAFT);
        lesson.setVersion(0L);

        TopicEntity topic = new TopicEntity();
        topic.setTopicId(topicId);
        topic.setPublicationState(PublicationState.PUBLISHED);

        SegmentEntity segment = new SegmentEntity();
        segment.setSegmentId(UUID.randomUUID());
        segment.setLessonId(lessonId);
        segment.setMediaAssetId(mediaId);
        segment.setPublicationState(PublicationState.PUBLISHED);

        MediaAssetEntity media = new MediaAssetEntity();
        media.setMediaAssetId(mediaId);
        media.setApprovalStatus(ApprovalStatus.APPROVED);

        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));
        when(segmentRepository.findByLessonIdOrderBySequenceNoAsc(lessonId)).thenReturn(List.of(segment));
        when(mediaAssetRepository.findById(mediaId)).thenReturn(Optional.of(media));
        when(lessonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LessonEntity published = publicationService.publishLesson(lessonId, 0L, actorId);

        assertEquals(PublicationState.PUBLISHED, published.getPublicationState());
        assertNotNull(published.getPublishedAt());
    }

    @Test
    void publishLesson_withAnyDraftSegment_throwsStateConflict() {
        UUID lessonId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId(lessonId); lesson.setTopicId(topicId); lesson.setAccessLevel(AccessLevel.FREE);
        lesson.setPublicationState(PublicationState.DRAFT); lesson.setVersion(0L);
        TopicEntity topic = new TopicEntity(); topic.setTopicId(topicId); topic.setPublicationState(PublicationState.PUBLISHED);
        SegmentEntity draft = new SegmentEntity(); draft.setSegmentId(UUID.randomUUID()); draft.setLessonId(lessonId);
        draft.setPublicationState(PublicationState.DRAFT); draft.setSequenceNo(1);
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));
        when(segmentRepository.findByLessonIdOrderBySequenceNoAsc(lessonId)).thenReturn(List.of(draft));

        assertThrows(StateConflictException.class, () -> publicationService.publishLesson(lessonId, 0L, actorId));
    }

    @Test
    void unpublishSegment_underPublishedLesson_throwsStateConflict() {
        UUID lessonId = UUID.randomUUID();
        UUID segmentId = UUID.randomUUID();
        SegmentEntity segment = new SegmentEntity();
        segment.setSegmentId(segmentId); segment.setLessonId(lessonId); segment.setPublicationState(PublicationState.PUBLISHED); segment.setVersion(2L);
        LessonEntity lesson = new LessonEntity(); lesson.setLessonId(lessonId); lesson.setPublicationState(PublicationState.PUBLISHED);
        when(segmentRepository.findById(segmentId)).thenReturn(Optional.of(segment));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));

        assertThrows(StateConflictException.class, () -> publicationService.unpublishSegment(segmentId, 2L, actorId));
    }

    @Test
    void archiveLesson_archivesEveryChildSegment() {
        UUID lessonId = UUID.randomUUID();
        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId(lessonId); lesson.setPublicationState(PublicationState.UNPUBLISHED); lesson.setVersion(1L);
        SegmentEntity first = new SegmentEntity(); first.setSegmentId(UUID.randomUUID()); first.setLessonId(lessonId);
        first.setPublicationState(PublicationState.PUBLISHED); first.setVersion(2L);
        SegmentEntity second = new SegmentEntity(); second.setSegmentId(UUID.randomUUID()); second.setLessonId(lessonId);
        second.setPublicationState(PublicationState.DRAFT); second.setVersion(4L);
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(segmentRepository.findByLessonIdOrderBySequenceNoAsc(lessonId)).thenReturn(List.of(first, second));
        when(segmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(lessonRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        publicationService.archiveLesson(lessonId, 1L, actorId);

        assertEquals(PublicationState.ARCHIVED, lesson.getPublicationState());
        assertEquals(PublicationState.ARCHIVED, first.getPublicationState());
        assertEquals(PublicationState.ARCHIVED, second.getPublicationState());
    }
}
