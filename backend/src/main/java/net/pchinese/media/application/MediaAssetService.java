package net.pchinese.media.application;

import net.pchinese.content.api.ResourceNotFoundException;
import net.pchinese.content.api.StateConflictException;
import net.pchinese.content.api.ContentValidationException;
import net.pchinese.content.application.ContentAuditService;
import net.pchinese.content.domain.PublicationState;
import net.pchinese.content.persistence.LessonEntity;
import net.pchinese.content.persistence.LessonRepository;
import net.pchinese.content.persistence.SegmentEntity;
import net.pchinese.content.persistence.SegmentRepository;
import net.pchinese.media.domain.ApprovalStatus;
import net.pchinese.media.domain.MediaKind;
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
public class MediaAssetService {

    private final MediaAssetRepository mediaAssetRepository;
    private final SegmentRepository segmentRepository;
    private final LessonRepository lessonRepository;
    private final MediaIntakePolicy intakePolicy;
    private final ContentAuditService auditService;

    public MediaAssetService(MediaAssetRepository mediaAssetRepository, SegmentRepository segmentRepository,
                             LessonRepository lessonRepository, MediaIntakePolicy intakePolicy,
                             ContentAuditService auditService) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.segmentRepository = segmentRepository;
        this.lessonRepository = lessonRepository;
        this.intakePolicy = intakePolicy;
        this.auditService = auditService;
    }

    @Transactional
    public MediaAssetEntity createYoutubeMediaAsset(String youtubeVideoReference, Integer durationMilliseconds,
                                                    String title, String altText, UUID actorUserId) {
        MediaIntakePolicy.IntakeReference intake = intakePolicy.youtubeReference(youtubeVideoReference);
        return createPendingAsset(intake, MediaKind.VIDEO, durationMilliseconds, title, altText, actorUserId);
    }

    private MediaAssetEntity createPendingAsset(MediaIntakePolicy.IntakeReference intake, MediaKind mediaKind,
                                                Integer durationMilliseconds, String title, String altText,
                                                UUID actorUserId) {
        if (mediaAssetRepository.existsByProviderNameAndProviderAssetIdentifier(
                intake.providerName(), intake.providerAssetIdentifier())) {
            throw new ContentValidationException("This YouTube video is already registered as a media asset.");
        }
        MediaAssetEntity media = new MediaAssetEntity();
        media.setMediaAssetId(UUID.randomUUID());
        media.setProviderName(intake.providerName());
        media.setProviderAssetIdentifier(intake.providerAssetIdentifier());
        media.setMediaKind(mediaKind);
        media.setMimeType(intake.mimeType());
        media.setDurationMilliseconds(durationMilliseconds);
        media.setTitle(title);
        media.setAltText(altText);
        // A validated YouTube reference enters trusted-source review before it may be approved.
        media.setApprovalStatus(ApprovalStatus.PENDING_SCAN);
        media.setMalwareScanStatus("PENDING");
        media.setCreatedByUserId(actorUserId);

        MediaAssetEntity saved = mediaAssetRepository.save(media);
        auditService.record("MEDIA_CREATE", actorUserId, "MEDIA_ASSET", saved.getMediaAssetId(), "SUCCESS", null,
                null, saved.getVersion(), null, saved.getApprovalStatus().name(), null);
        return saved;
    }

    @Transactional
    public MediaAssetEntity updateMediaAsset(UUID mediaAssetId, String title, String altText,
                                             Long expectedVersion, UUID actorUserId) {
        MediaAssetEntity media = mediaAssetRepository.findById(mediaAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset not found: " + mediaAssetId));

        if (!Objects.equals(media.getVersion(), expectedVersion)) {
            throw new StateConflictException("Stale media version. Expected: " + expectedVersion + ", actual: " + media.getVersion());
        }
        assertNotReferencedByPublishedLesson(media, actorUserId, expectedVersion);

        String beforeState = media.getApprovalStatus().name();
        if (title != null) media.setTitle(title);
        if (altText != null) media.setAltText(altText);
        MediaAssetEntity saved = mediaAssetRepository.save(media);
        auditService.record("MEDIA_UPDATE", actorUserId, "MEDIA_ASSET", saved.getMediaAssetId(), "SUCCESS", null,
                expectedVersion, saved.getVersion(), beforeState, saved.getApprovalStatus().name(), null);
        return saved;
    }

    public Page<MediaAssetEntity> listMediaAssets(Pageable pageable) {
        return mediaAssetRepository.findAll(pageable);
    }

    public MediaAssetEntity getMediaAsset(UUID mediaAssetId) {
        return mediaAssetRepository.findById(mediaAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset not found: " + mediaAssetId));
    }

    private void assertNotReferencedByPublishedLesson(MediaAssetEntity media, UUID actorUserId, Long expectedVersion) {
        for (SegmentEntity segment : segmentRepository.findByMediaAssetId(media.getMediaAssetId())) {
            LessonEntity lesson = lessonRepository.findById(segment.getLessonId())
                    .orElseThrow(() -> new StateConflictException("A media segment has no parent lesson."));
            if (lesson.getPublicationState() == PublicationState.PUBLISHED) {
                auditService.recordRejected("MEDIA_UPDATE", actorUserId, "MEDIA_ASSET", media.getMediaAssetId(),
                        "MEDIA_REFERENCED_BY_PUBLISHED_LESSON", expectedVersion, media.getVersion(),
                        media.getApprovalStatus().name(), null);
                throw new StateConflictException("Must unpublish the parent lesson before editing its learner-visible media.");
            }
        }
    }
}
