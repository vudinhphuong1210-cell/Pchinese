package net.pchinese.content.application;

import net.pchinese.content.api.ContentValidationException;
import net.pchinese.content.api.ResourceNotFoundException;
import net.pchinese.content.api.StateConflictException;
import net.pchinese.content.domain.AccessLevel;
import net.pchinese.content.domain.PublicationState;
import net.pchinese.content.domain.SegmentType;
import net.pchinese.content.persistence.*;
import net.pchinese.media.domain.ApprovalStatus;
import net.pchinese.media.persistence.MediaAssetEntity;
import net.pchinese.media.persistence.MediaAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ContentDraftService {

    private final TopicRepository topicRepository;
    private final LessonRepository lessonRepository;
    private final SegmentRepository segmentRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final ContentAuditService auditService;

    public ContentDraftService(TopicRepository topicRepository,
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

    // --- Topic ---

    @Transactional
    public TopicEntity createTopic(String title, String slug, String description, Integer hskLevel, Integer sortOrder, UUID actorUserId) {
        if (topicRepository.existsBySlug(slug)) {
            throw new ContentValidationException("Topic slug already exists: " + slug);
        }

        TopicEntity topic = new TopicEntity();
        topic.setTopicId(UUID.randomUUID());
        topic.setTitle(title);
        topic.setSlug(slug);
        topic.setDescription(description);
        topic.setHskLevel(hskLevel != null ? hskLevel.shortValue() : null);
        topic.setSortOrder(sortOrder != null ? sortOrder : 0);
        topic.setPublicationState(PublicationState.DRAFT);
        topic.setCreatedByUserId(actorUserId);
        topic.setUpdatedByUserId(actorUserId);

        TopicEntity saved = topicRepository.save(topic);
        auditService.record("TOPIC_CREATE", actorUserId, "TOPIC", saved.getTopicId(), "SUCCESS", null,
                null, saved.getVersion(), null, saved.getPublicationState().name(), null);
        return saved;
    }

    @Transactional
    public TopicEntity updateTopic(UUID topicId, String title, String slug, String description, Integer hskLevel, Integer sortOrder, Long expectedVersion, UUID actorUserId) {
        TopicEntity topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));

        if (topic.getPublicationState() == PublicationState.ARCHIVED) {
            throw new StateConflictException("Cannot update an ARCHIVED topic.");
        }

        if (!Objects.equals(topic.getVersion(), expectedVersion)) {
            throw new StateConflictException("Stale topic version. Expected: " + expectedVersion + ", actual: " + topic.getVersion());
        }

        if (slug != null && !slug.equals(topic.getSlug()) && topicRepository.existsBySlug(slug)) {
            throw new ContentValidationException("Topic slug already exists: " + slug);
        }

        String beforeState = topic.getPublicationState().name();
        if (title != null) topic.setTitle(title);
        if (slug != null) topic.setSlug(slug);
        if (description != null) topic.setDescription(description);
        if (hskLevel != null) topic.setHskLevel(hskLevel.shortValue());
        if (sortOrder != null) topic.setSortOrder(sortOrder);
        topic.setUpdatedByUserId(actorUserId);

        TopicEntity saved = topicRepository.save(topic);
        auditService.record("TOPIC_UPDATE", actorUserId, "TOPIC", saved.getTopicId(), "SUCCESS", null,
                expectedVersion, saved.getVersion(), beforeState, saved.getPublicationState().name(), null);
        return saved;
    }

    public Page<TopicEntity> listTopics(Pageable pageable) {
        return topicRepository.findAll(pageable);
    }

    public TopicEntity getTopic(UUID topicId) {
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + topicId));
    }

    // --- Lesson ---

    @Transactional
    public LessonEntity createLesson(UUID topicId, String title, String slug, String summary, Integer hskLevel, Integer sortOrder, AccessLevel accessLevel, UUID actorUserId) {
        TopicEntity topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent topic not found: " + topicId));

        if (topic.getPublicationState() == PublicationState.ARCHIVED) {
            throw new StateConflictException("Cannot add lesson to an ARCHIVED topic.");
        }

        if (lessonRepository.existsBySlug(slug)) {
            throw new ContentValidationException("Lesson slug already exists: " + slug);
        }

        if (accessLevel != null && accessLevel != AccessLevel.FREE) {
            throw new ContentValidationException("Only FREE access level is permitted for learning content.");
        }

        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId(UUID.randomUUID());
        lesson.setTopicId(topicId);
        lesson.setTitle(title);
        lesson.setSlug(slug);
        lesson.setSummary(summary);
        lesson.setHskLevel(hskLevel != null ? hskLevel.shortValue() : topic.getHskLevel());
        lesson.setAccessLevel(AccessLevel.FREE);
        lesson.setPublicationState(PublicationState.DRAFT);
        lesson.setSortOrder(sortOrder != null ? sortOrder : 0);
        lesson.setCreatedByUserId(actorUserId);
        lesson.setUpdatedByUserId(actorUserId);

        LessonEntity saved = lessonRepository.save(lesson);
        auditService.record("LESSON_CREATE", actorUserId, "LESSON", saved.getLessonId(), "SUCCESS", null,
                null, saved.getVersion(), null, saved.getPublicationState().name(), null);
        return saved;
    }

    @Transactional
    public LessonEntity updateLesson(UUID lessonId, String title, String summary, Integer sortOrder, Long expectedVersion, UUID actorUserId) {
        LessonEntity lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + lessonId));

        if (lesson.getPublicationState() == PublicationState.PUBLISHED) {
            throw new StateConflictException("Must unpublish lesson before editing draft fields.");
        }
        if (lesson.getPublicationState() == PublicationState.ARCHIVED) {
            throw new StateConflictException("Cannot update an ARCHIVED lesson.");
        }

        if (!Objects.equals(lesson.getVersion(), expectedVersion)) {
            throw new StateConflictException("Stale lesson version. Expected: " + expectedVersion + ", actual: " + lesson.getVersion());
        }

        String beforeState = lesson.getPublicationState().name();
        if (title != null) lesson.setTitle(title);
        if (summary != null) lesson.setSummary(summary);
        if (sortOrder != null) lesson.setSortOrder(sortOrder);
        lesson.setUpdatedByUserId(actorUserId);

        LessonEntity saved = lessonRepository.save(lesson);
        auditService.record("LESSON_UPDATE", actorUserId, "LESSON", saved.getLessonId(), "SUCCESS", null,
                expectedVersion, saved.getVersion(), beforeState, saved.getPublicationState().name(), null);
        return saved;
    }

    public Page<LessonEntity> listLessons(Pageable pageable) {
        return lessonRepository.findAll(pageable);
    }

    public LessonEntity getLesson(UUID lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + lessonId));
    }

    // --- Segment ---

    @Transactional
    public SegmentEntity createSegment(UUID lessonId, UUID mediaAssetId, Integer sequenceNo, SegmentType segmentType,
                                        Integer startMs, Integer endMs, String hanzi, String pinyin, String translationVi,
                                        String hint, UUID actorUserId) {
        LessonEntity lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent lesson not found: " + lessonId));

        if (lesson.getPublicationState() == PublicationState.ARCHIVED) {
            throw new StateConflictException("Cannot add segment to an ARCHIVED lesson.");
        }
        if (lesson.getPublicationState() == PublicationState.PUBLISHED) {
            throw new StateConflictException("Must unpublish lesson before adding a learner-visible segment.");
        }

        MediaAssetEntity media = mediaAssetRepository.findById(mediaAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset not found: " + mediaAssetId));

        if (media.getApprovalStatus() == ApprovalStatus.REJECTED || media.getApprovalStatus() == ApprovalStatus.QUARANTINED) {
            throw new StateConflictException("Referenced media asset is REJECTED or QUARANTINED.");
        }

        if (segmentRepository.findByLessonIdAndSequenceNo(lessonId, sequenceNo).isPresent()) {
            throw new ContentValidationException("Segment sequence number " + sequenceNo + " already exists in lesson.");
        }

        SegmentEntity segment = new SegmentEntity();
        segment.setSegmentId(UUID.randomUUID());
        segment.setLessonId(lessonId);
        segment.setMediaAssetId(mediaAssetId);
        segment.setSequenceNo(sequenceNo);
        segment.setSegmentType(segmentType != null ? segmentType : SegmentType.BOTH);
        segment.setPublicationState(PublicationState.DRAFT);
        segment.setStartMilliseconds(startMs != null ? startMs : 0);
        segment.setEndMilliseconds(endMs != null ? endMs : 0);
        segment.setTranscriptHanzi(hanzi);
        segment.setTranscriptPinyin(pinyin);
        segment.setTranslationVi(translationVi);
        segment.setDictationHint(hint);
        segment.setCreatedByUserId(actorUserId);
        segment.setUpdatedByUserId(actorUserId);

        SegmentEntity saved = segmentRepository.save(segment);
        auditService.record("SEGMENT_CREATE", actorUserId, "SEGMENT", saved.getSegmentId(), "SUCCESS", null,
                null, saved.getVersion(), null, saved.getPublicationState().name(), null);
        return saved;
    }

    @Transactional
    public SegmentEntity updateSegment(UUID segmentId, UUID mediaAssetId, Integer sequenceNo, String hanzi, String pinyin,
                                        String translationVi, String hint, Long expectedVersion, UUID actorUserId) {
        SegmentEntity segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Segment not found: " + segmentId));

        if (segment.getPublicationState() == PublicationState.PUBLISHED) {
            throw new StateConflictException("Must unpublish segment before editing.");
        }
        if (segment.getPublicationState() == PublicationState.ARCHIVED) {
            throw new StateConflictException("Cannot update an ARCHIVED segment.");
        }
        LessonEntity parentLesson = lessonRepository.findById(segment.getLessonId())
                .orElseThrow(() -> new StateConflictException("Parent lesson does not exist."));
        if (parentLesson.getPublicationState() == PublicationState.PUBLISHED) {
            throw new StateConflictException("Must unpublish lesson before editing a learner-visible segment.");
        }

        if (!Objects.equals(segment.getVersion(), expectedVersion)) {
            throw new StateConflictException("Stale segment version. Expected: " + expectedVersion + ", actual: " + segment.getVersion());
        }

        String beforeState = segment.getPublicationState().name();
        if (mediaAssetId != null && !mediaAssetId.equals(segment.getMediaAssetId())) {
            MediaAssetEntity media = mediaAssetRepository.findById(mediaAssetId)
                    .orElseThrow(() -> new ResourceNotFoundException("Media asset not found: " + mediaAssetId));
            if (media.getApprovalStatus() == ApprovalStatus.REJECTED || media.getApprovalStatus() == ApprovalStatus.QUARANTINED) {
                throw new StateConflictException("Referenced media asset is REJECTED or QUARANTINED.");
            }
            segment.setMediaAssetId(mediaAssetId);
        }

        if (sequenceNo != null && !sequenceNo.equals(segment.getSequenceNo())) {
            var existing = segmentRepository.findByLessonIdAndSequenceNo(segment.getLessonId(), sequenceNo);
            if (existing.isPresent() && !existing.get().getSegmentId().equals(segmentId)) {
                throw new ContentValidationException("Sequence number " + sequenceNo + " already exists in lesson.");
            }
            segment.setSequenceNo(sequenceNo);
        }

        if (hanzi != null) segment.setTranscriptHanzi(hanzi);
        if (pinyin != null) segment.setTranscriptPinyin(pinyin);
        if (translationVi != null) segment.setTranslationVi(translationVi);
        if (hint != null) segment.setDictationHint(hint);
        segment.setUpdatedByUserId(actorUserId);

        SegmentEntity saved = segmentRepository.save(segment);
        auditService.record("SEGMENT_UPDATE", actorUserId, "SEGMENT", saved.getSegmentId(), "SUCCESS", null,
                expectedVersion, saved.getVersion(), beforeState, saved.getPublicationState().name(), null);
        return saved;
    }

    public Page<SegmentEntity> listSegments(Pageable pageable) {
        return segmentRepository.findAll(pageable);
    }

    public SegmentEntity getSegment(UUID segmentId) {
        return segmentRepository.findById(segmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Segment not found: " + segmentId));
    }
}
