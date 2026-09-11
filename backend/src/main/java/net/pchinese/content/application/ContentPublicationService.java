package net.pchinese.content.application;

import net.pchinese.content.api.ResourceNotFoundException;
import net.pchinese.content.api.StateConflictException;
import net.pchinese.content.domain.AccessLevel;
import net.pchinese.content.domain.PublicationState;
import net.pchinese.content.persistence.*;
import net.pchinese.media.domain.ApprovalStatus;
import net.pchinese.media.persistence.MediaAssetEntity;
import net.pchinese.media.persistence.MediaAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ContentPublicationService {

    private final TopicRepository topicRepository;
    private final LessonRepository lessonRepository;
    private final SegmentRepository segmentRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final ContentAuditService auditService;

    public ContentPublicationService(TopicRepository topicRepository,
                                     LessonRepository lessonRepository,
                                     SegmentRepository segmentRepository,
                                     MediaAssetRepository mediaAssetRepository,
                                     ContentAuditService auditService) {
        this.topicRepository = topicRepository;
        this.lessonRepository = lessonRepository;
        this.segmentRepository = segmentRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.auditService = auditService;
    }

    // --- Topic Lifecycle ---

    @Transactional
    public TopicEntity publishTopic(UUID topicId, Long expectedVersion, UUID actorUserId) {
        TopicEntity topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));

        if (!Objects.equals(topic.getVersion(), expectedVersion)) {
            throw new StateConflictException("Stale topic version. Expected: " + expectedVersion + ", actual: " + topic.getVersion());
        }
        if (topic.getPublicationState() == PublicationState.ARCHIVED) {
            throw new StateConflictException("Cannot publish an ARCHIVED topic.");
        }

        String beforeState = topic.getPublicationState().name();
        topic.setPublicationState(PublicationState.PUBLISHED);
        topic.setPublishedAt(Instant.now());
        topic.setUpdatedByUserId(actorUserId);

        TopicEntity saved = topicRepository.save(topic);
        recordLifecycle("TOPIC_PUBLISH", actorUserId, "TOPIC", saved.getTopicId(), expectedVersion, saved.getVersion(), beforeState, saved.getPublicationState().name());
        return saved;
    }

    @Transactional
    public TopicEntity unpublishTopic(UUID topicId, Long expectedVersion, UUID actorUserId) {
        TopicEntity topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));

        if (!Objects.equals(topic.getVersion(), expectedVersion)) {
            throw new StateConflictException("Stale topic version. Expected: " + expectedVersion + ", actual: " + topic.getVersion());
        }
        if (topic.getPublicationState() == PublicationState.ARCHIVED) {
            throw new StateConflictException("Cannot unpublish an ARCHIVED topic.");
        }

        String beforeState = topic.getPublicationState().name();
        topic.setPublicationState(PublicationState.UNPUBLISHED);
        topic.setUpdatedByUserId(actorUserId);

        // Cascade unpublish published child lessons
        List<LessonEntity> childLessons = lessonRepository.findByTopicIdAndPublicationState(topicId, PublicationState.PUBLISHED);
        for (LessonEntity lesson : childLessons) {
            String lessonBeforeState = lesson.getPublicationState().name();
            Long lessonBeforeVersion = lesson.getVersion();
            lesson.setPublicationState(PublicationState.UNPUBLISHED);
            lesson.setUpdatedByUserId(actorUserId);
            lessonRepository.save(lesson);
            recordLifecycle("LESSON_UNPUBLISH_CASCADE", actorUserId, "LESSON", lesson.getLessonId(), lessonBeforeVersion,
                    lesson.getVersion(), lessonBeforeState, lesson.getPublicationState().name());
        }

        TopicEntity saved = topicRepository.save(topic);
        recordLifecycle("TOPIC_UNPUBLISH", actorUserId, "TOPIC", saved.getTopicId(), expectedVersion, saved.getVersion(), beforeState, saved.getPublicationState().name());
        return saved;
    }

    @Transactional
    public TopicEntity archiveTopic(UUID topicId, Long expectedVersion, UUID actorUserId) {
        TopicEntity topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));

        if (!Objects.equals(topic.getVersion(), expectedVersion)) {
            throw new StateConflictException("Stale topic version. Expected: " + expectedVersion + ", actual: " + topic.getVersion());
        }

        String beforeState = topic.getPublicationState().name();
        topic.setPublicationState(PublicationState.ARCHIVED);
        topic.setUpdatedByUserId(actorUserId);

        // Cascade archive lessons
        List<LessonEntity> childLessons = lessonRepository.findByTopicId(topicId);
        for (LessonEntity lesson : childLessons) {
            if (lesson.getPublicationState() != PublicationState.ARCHIVED) {
                String lessonBeforeState = lesson.getPublicationState().name();
                Long lessonBeforeVersion = lesson.getVersion();
                lesson.setPublicationState(PublicationState.ARCHIVED);
                lesson.setUpdatedByUserId(actorUserId);
                lessonRepository.save(lesson);
                recordLifecycle("LESSON_ARCHIVE_CASCADE", actorUserId, "LESSON", lesson.getLessonId(), lessonBeforeVersion,
                        lesson.getVersion(), lessonBeforeState, lesson.getPublicationState().name());
            }
            for (SegmentEntity segment : segmentRepository.findByLessonIdOrderBySequenceNoAsc(lesson.getLessonId())) {
                if (segment.getPublicationState() != PublicationState.ARCHIVED) {
                    String segmentBeforeState = segment.getPublicationState().name();
                    Long segmentBeforeVersion = segment.getVersion();
                    segment.setPublicationState(PublicationState.ARCHIVED);
                    segment.setUpdatedByUserId(actorUserId);
                    segmentRepository.save(segment);
                    recordLifecycle("SEGMENT_ARCHIVE_CASCADE", actorUserId, "SEGMENT", segment.getSegmentId(), segmentBeforeVersion,
                            segment.getVersion(), segmentBeforeState, segment.getPublicationState().name());
                }
            }
        }

        TopicEntity saved = topicRepository.save(topic);
        recordLifecycle("TOPIC_ARCHIVE", actorUserId, "TOPIC", saved.getTopicId(), expectedVersion, saved.getVersion(), beforeState, saved.getPublicationState().name());
        return saved;
    }

    // --- Lesson Lifecycle ---

    @Transactional
    public LessonEntity publishLesson(UUID lessonId, Long expectedVersion, UUID actorUserId) {
        LessonEntity lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + lessonId));

        if (!Objects.equals(lesson.getVersion(), expectedVersion)) {
            throw new StateConflictException("Stale lesson version. Expected: " + expectedVersion + ", actual: " + lesson.getVersion());
        }
        if (lesson.getPublicationState() == PublicationState.ARCHIVED) {
            throw new StateConflictException("Cannot publish an ARCHIVED lesson.");
        }

        // Prerequisite 1: Parent topic must be PUBLISHED
        TopicEntity parentTopic = topicRepository.findById(lesson.getTopicId())
                .orElseThrow(() -> new StateConflictException("Parent topic does not exist."));
        if (parentTopic.getPublicationState() != PublicationState.PUBLISHED) {
            throw new StateConflictException("Cannot publish lesson because parent topic is not PUBLISHED (current: " + parentTopic.getPublicationState() + ").");
        }

        // Prerequisite 2: Access level must be FREE
        if (lesson.getAccessLevel() != AccessLevel.FREE) {
            throw new StateConflictException("Cannot publish lesson with non-FREE access level.");
        }

        // Prerequisite 3: Must have at least one segment and every segment must be ready.
        List<SegmentEntity> lessonSegments = segmentRepository.findByLessonIdOrderBySequenceNoAsc(lessonId);
        if (lessonSegments.isEmpty()) {
            throw new StateConflictException("Cannot publish lesson with 0 segments.");
        }

        // Prerequisite 4: Every associated segment is PUBLISHED and uses APPROVED media.
        for (SegmentEntity segment : lessonSegments) {
            if (segment.getPublicationState() != PublicationState.PUBLISHED) {
                throw new StateConflictException("Cannot publish lesson because segment " + segment.getSequenceNo()
                        + " is not PUBLISHED (current: " + segment.getPublicationState() + ").");
            }
            MediaAssetEntity media = mediaAssetRepository.findById(segment.getMediaAssetId())
                    .orElseThrow(() -> new StateConflictException("Segment media asset not found: " + segment.getMediaAssetId()));
            if (media.getApprovalStatus() != ApprovalStatus.APPROVED) {
                throw new StateConflictException("Cannot publish lesson because segment " + segment.getSequenceNo() + " references unapproved media (" + media.getApprovalStatus() + ").");
            }
        }

        String beforeState = lesson.getPublicationState().name();
        lesson.setPublicationState(PublicationState.PUBLISHED);
        lesson.setPublishedAt(Instant.now());
        lesson.setUpdatedByUserId(actorUserId);

        LessonEntity saved = lessonRepository.save(lesson);
        recordLifecycle("LESSON_PUBLISH", actorUserId, "LESSON", saved.getLessonId(), expectedVersion, saved.getVersion(), beforeState, saved.getPublicationState().name());
        return saved;
    }

    @Transactional
    public LessonEntity unpublishLesson(UUID lessonId, Long expectedVersion, UUID actorUserId) {
        LessonEntity lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + lessonId));

        if (!Objects.equals(lesson.getVersion(), expectedVersion)) {
            throw new StateConflictException("Stale lesson version. Expected: " + expectedVersion + ", actual: " + lesson.getVersion());
        }
        if (lesson.getPublicationState() == PublicationState.ARCHIVED) {
            throw new StateConflictException("Cannot unpublish an ARCHIVED lesson.");
        }

        String beforeState = lesson.getPublicationState().name();
        lesson.setPublicationState(PublicationState.UNPUBLISHED);
        lesson.setUpdatedByUserId(actorUserId);

        LessonEntity saved = lessonRepository.save(lesson);
        recordLifecycle("LESSON_UNPUBLISH", actorUserId, "LESSON", saved.getLessonId(), expectedVersion, saved.getVersion(), beforeState, saved.getPublicationState().name());
        return saved;
    }

    @Transactional
    public LessonEntity archiveLesson(UUID lessonId, Long expectedVersion, UUID actorUserId) {
        LessonEntity lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + lessonId));

        if (!Objects.equals(lesson.getVersion(), expectedVersion)) {
            throw new StateConflictException("Stale lesson version. Expected: " + expectedVersion + ", actual: " + lesson.getVersion());
        }

        String beforeState = lesson.getPublicationState().name();
        lesson.setPublicationState(PublicationState.ARCHIVED);
        lesson.setUpdatedByUserId(actorUserId);

        for (SegmentEntity segment : segmentRepository.findByLessonIdOrderBySequenceNoAsc(lessonId)) {
            if (segment.getPublicationState() != PublicationState.ARCHIVED) {
                String segmentBeforeState = segment.getPublicationState().name();
                Long segmentBeforeVersion = segment.getVersion();
                segment.setPublicationState(PublicationState.ARCHIVED);
                segment.setUpdatedByUserId(actorUserId);
                segmentRepository.save(segment);
                recordLifecycle("SEGMENT_ARCHIVE_CASCADE", actorUserId, "SEGMENT", segment.getSegmentId(), segmentBeforeVersion,
                        segment.getVersion(), segmentBeforeState, segment.getPublicationState().name());
            }
        }

        LessonEntity saved = lessonRepository.save(lesson);
        recordLifecycle("LESSON_ARCHIVE", actorUserId, "LESSON", saved.getLessonId(), expectedVersion, saved.getVersion(), beforeState, saved.getPublicationState().name());
        return saved;
    }

    // --- Segment Lifecycle ---

    @Transactional
    public SegmentEntity publishSegment(UUID segmentId, Long expectedVersion, UUID actorUserId) {
        SegmentEntity segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Segment not found: " + segmentId));

        if (!Objects.equals(segment.getVersion(), expectedVersion)) {
            throw new StateConflictException("Stale segment version. Expected: " + expectedVersion + ", actual: " + segment.getVersion());
        }
        if (segment.getPublicationState() == PublicationState.ARCHIVED) {
            throw new StateConflictException("Cannot publish an ARCHIVED segment.");
        }
        LessonEntity parentLesson = lessonRepository.findById(segment.getLessonId())
                .orElseThrow(() -> new StateConflictException("Parent lesson does not exist."));
        if (parentLesson.getPublicationState() == PublicationState.ARCHIVED) {
            throw new StateConflictException("Cannot publish a segment in an ARCHIVED lesson.");
        }

        MediaAssetEntity media = mediaAssetRepository.findById(segment.getMediaAssetId())
                .orElseThrow(() -> new StateConflictException("Referenced media asset not found."));
        if (media.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new StateConflictException("Cannot publish segment when referenced media asset status is " + media.getApprovalStatus());
        }

        String beforeState = segment.getPublicationState().name();
        segment.setPublicationState(PublicationState.PUBLISHED);
        segment.setPublishedAt(Instant.now());
        segment.setUpdatedByUserId(actorUserId);

        SegmentEntity saved = segmentRepository.save(segment);
        recordLifecycle("SEGMENT_PUBLISH", actorUserId, "SEGMENT", saved.getSegmentId(), expectedVersion, saved.getVersion(), beforeState, saved.getPublicationState().name());
        return saved;
    }

    @Transactional
    public SegmentEntity unpublishSegment(UUID segmentId, Long expectedVersion, UUID actorUserId) {
        SegmentEntity segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Segment not found: " + segmentId));

        if (!Objects.equals(segment.getVersion(), expectedVersion)) {
            throw new StateConflictException("Stale segment version. Expected: " + expectedVersion + ", actual: " + segment.getVersion());
        }
        if (segment.getPublicationState() == PublicationState.ARCHIVED) {
            throw new StateConflictException("Cannot unpublish an ARCHIVED segment.");
        }
        assertParentLessonIsNotPublished(segment.getLessonId());

        String beforeState = segment.getPublicationState().name();
        segment.setPublicationState(PublicationState.UNPUBLISHED);
        segment.setUpdatedByUserId(actorUserId);

        SegmentEntity saved = segmentRepository.save(segment);
        recordLifecycle("SEGMENT_UNPUBLISH", actorUserId, "SEGMENT", saved.getSegmentId(), expectedVersion, saved.getVersion(), beforeState, saved.getPublicationState().name());
        return saved;
    }

    @Transactional
    public SegmentEntity archiveSegment(UUID segmentId, Long expectedVersion, UUID actorUserId) {
        SegmentEntity segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Segment not found: " + segmentId));

        if (!Objects.equals(segment.getVersion(), expectedVersion)) {
            throw new StateConflictException("Stale segment version. Expected: " + expectedVersion + ", actual: " + segment.getVersion());
        }
        assertParentLessonIsNotPublished(segment.getLessonId());

        String beforeState = segment.getPublicationState().name();
        segment.setPublicationState(PublicationState.ARCHIVED);
        segment.setUpdatedByUserId(actorUserId);

        SegmentEntity saved = segmentRepository.save(segment);
        recordLifecycle("SEGMENT_ARCHIVE", actorUserId, "SEGMENT", saved.getSegmentId(), expectedVersion, saved.getVersion(), beforeState, saved.getPublicationState().name());
        return saved;
    }

    private void assertParentLessonIsNotPublished(UUID lessonId) {
        LessonEntity lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new StateConflictException("Parent lesson does not exist."));
        if (lesson.getPublicationState() == PublicationState.PUBLISHED) {
            throw new StateConflictException("Must unpublish the parent lesson before changing a learner-visible segment.");
        }
    }

    private void recordLifecycle(String eventType, UUID actorUserId, String targetType, UUID targetId,
                                 Long expectedVersion, Long observedVersion, String beforeState, String afterState) {
        auditService.record(eventType, actorUserId, targetType, targetId, "SUCCESS", null,
                expectedVersion, observedVersion, beforeState, afterState, null);
    }
}
