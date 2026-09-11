package net.pchinese.content.api;

import net.pchinese.content.persistence.LessonEntity;
import net.pchinese.content.persistence.SegmentEntity;
import net.pchinese.content.persistence.TopicEntity;
import net.pchinese.media.persistence.MediaAssetEntity;

import java.util.UUID;

/** Safe API shape shared by F04 administration lists and individual reloads. */
public record AdminContentProjection(
        UUID id, String entityType, String publicationStatus, Long version, String title,
        UUID parentId, Integer sequenceNo, String accessLevel, String providerName, String youtubeVideoId,
        String slug, String description, String summary, Integer hskLevel,
        String transcriptHanzi, String transcriptPinyin, String translationVi, String dictationHint,
        Integer startMilliseconds, Integer endMilliseconds, String segmentType, String mediaKind,
        String altText, Boolean approvalEligible
) {
    public static AdminContentProjection from(TopicEntity topic) {
        return new AdminContentProjection(topic.getTopicId(), "TOPIC", topic.getPublicationState().name(), topic.getVersion(),
                topic.getTitle(), null, null, null, null, null, topic.getSlug(), topic.getDescription(), null,
                topic.getHskLevel() == null ? null : topic.getHskLevel().intValue(), null, null, null, null,
                null, null, null, null, null, null);
    }

    public static AdminContentProjection from(LessonEntity lesson) {
        return new AdminContentProjection(lesson.getLessonId(), "LESSON", lesson.getPublicationState().name(), lesson.getVersion(),
                lesson.getTitle(), lesson.getTopicId(), null, lesson.getAccessLevel().name(), null, null, lesson.getSlug(), null,
                lesson.getSummary(), lesson.getHskLevel() == null ? null : lesson.getHskLevel().intValue(), null, null,
                null, null, null, null, null, null, null, null);
    }

    public static AdminContentProjection from(SegmentEntity segment) {
        return new AdminContentProjection(segment.getSegmentId(), "SEGMENT", segment.getPublicationState().name(), segment.getVersion(),
                null, segment.getLessonId(), segment.getSequenceNo(), null, null, null, null, null, null, null,
                segment.getTranscriptHanzi(), segment.getTranscriptPinyin(), segment.getTranslationVi(), segment.getDictationHint(),
                segment.getStartMilliseconds(), segment.getEndMilliseconds(), segment.getSegmentType().name(), null, null, null);
    }

    public static AdminContentProjection from(MediaAssetEntity media) {
        return new AdminContentProjection(media.getMediaAssetId(), "MEDIA", media.getApprovalStatus().name(), media.getVersion(),
                media.getTitle(), null, null, null, media.getProviderName(), media.getProviderAssetIdentifier(), null, null, null, null,
                null, null, null, null, null, null, null, media.getMediaKind().name(), media.getAltText(),
                media.getApprovalStatus().name().equals("PENDING_SCAN") && "CLEAN".equals(media.getMalwareScanStatus()));
    }
}
