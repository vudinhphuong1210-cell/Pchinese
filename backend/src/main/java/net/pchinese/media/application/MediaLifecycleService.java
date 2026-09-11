package net.pchinese.media.application;

import net.pchinese.content.api.ResourceNotFoundException;
import net.pchinese.content.api.StateConflictException;
import net.pchinese.content.application.ContentAuditService;
import net.pchinese.content.domain.PublicationState;
import net.pchinese.content.persistence.LessonEntity;
import net.pchinese.content.persistence.LessonRepository;
import net.pchinese.content.persistence.SegmentEntity;
import net.pchinese.content.persistence.SegmentRepository;
import net.pchinese.media.domain.ApprovalStatus;
import net.pchinese.media.persistence.MediaAssetEntity;
import net.pchinese.media.persistence.MediaAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class MediaLifecycleService {

    private final MediaAssetRepository mediaAssetRepository;
    private final SegmentRepository segmentRepository;
    private final LessonRepository lessonRepository;
    private final ContentAuditService auditService;

    public MediaLifecycleService(MediaAssetRepository mediaAssetRepository,
                                 SegmentRepository segmentRepository,
                                 LessonRepository lessonRepository,
                                 ContentAuditService auditService) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.segmentRepository = segmentRepository;
        this.lessonRepository = lessonRepository;
        this.auditService = auditService;
    }

    @Transactional
    public MediaAssetEntity approveMedia(UUID mediaAssetId, Long expectedVersion, UUID actorUserId) {
        MediaAssetEntity media = mediaAssetRepository.findById(mediaAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset not found: " + mediaAssetId));

        if (!Objects.equals(media.getVersion(), expectedVersion)) {
            auditService.recordRejected("MEDIA_APPROVE", actorUserId, "MEDIA_ASSET", mediaAssetId,
                    "STALE_VERSION", expectedVersion, media.getVersion(), media.getApprovalStatus().name(), null);
            throw new StateConflictException("Stale media version. Expected: " + expectedVersion + ", actual: " + media.getVersion());
        }
        if (media.getApprovalStatus() != ApprovalStatus.PENDING_SCAN) {
            auditService.recordRejected("MEDIA_APPROVE", actorUserId, "MEDIA_ASSET", mediaAssetId,
                    "INVALID_MEDIA_TRANSITION", expectedVersion, media.getVersion(), media.getApprovalStatus().name(), null);
            throw new StateConflictException("Only PENDING_SCAN media can be approved; rejected and quarantined media are terminal.");
        }
        if (!"CLEAN".equals(media.getMalwareScanStatus())) {
            auditService.recordRejected("MEDIA_APPROVE", actorUserId, "MEDIA_ASSET", mediaAssetId,
                    "MEDIA_SCAN_NOT_CLEAN", expectedVersion, media.getVersion(), media.getApprovalStatus().name(), null);
            throw new StateConflictException("Media must have a CLEAN safety scan before approval.");
        }

        String beforeState = media.getApprovalStatus().name();
        media.setApprovalStatus(ApprovalStatus.APPROVED);
        media.setApprovedByUserId(actorUserId);
        media.setApprovedAt(Instant.now());

        MediaAssetEntity saved = mediaAssetRepository.save(media);
        auditService.record("MEDIA_APPROVE", actorUserId, "MEDIA_ASSET", saved.getMediaAssetId(), "SUCCESS", null,
                expectedVersion, saved.getVersion(), beforeState, saved.getApprovalStatus().name(), null);
        return saved;
    }

    @Transactional
    public MediaAssetEntity rejectMedia(UUID mediaAssetId, Long expectedVersion, UUID actorUserId) {
        return changeMediaStatusAndWithdrawDependentLessons(mediaAssetId, expectedVersion, ApprovalStatus.REJECTED, "MEDIA_REJECT", actorUserId);
    }

    @Transactional
    public MediaAssetEntity quarantineMedia(UUID mediaAssetId, Long expectedVersion, UUID actorUserId) {
        return changeMediaStatusAndWithdrawDependentLessons(mediaAssetId, expectedVersion, ApprovalStatus.QUARANTINED, "MEDIA_QUARANTINE", actorUserId);
    }

    private MediaAssetEntity changeMediaStatusAndWithdrawDependentLessons(UUID mediaAssetId, Long expectedVersion, ApprovalStatus targetStatus, String eventType, UUID actorUserId) {
        MediaAssetEntity media = mediaAssetRepository.findById(mediaAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("Media asset not found: " + mediaAssetId));

        if (!Objects.equals(media.getVersion(), expectedVersion)) {
            auditService.recordRejected(eventType, actorUserId, "MEDIA_ASSET", mediaAssetId,
                    "STALE_VERSION", expectedVersion, media.getVersion(), media.getApprovalStatus().name(), null);
            throw new StateConflictException("Stale media version. Expected: " + expectedVersion + ", actual: " + media.getVersion());
        }
        if (media.getApprovalStatus() == ApprovalStatus.REJECTED || media.getApprovalStatus() == ApprovalStatus.QUARANTINED) {
            auditService.recordRejected(eventType, actorUserId, "MEDIA_ASSET", mediaAssetId,
                    "INVALID_MEDIA_TRANSITION", expectedVersion, media.getVersion(), media.getApprovalStatus().name(), null);
            throw new StateConflictException("Rejected and quarantined media are terminal and cannot transition again.");
        }

        String beforeState = media.getApprovalStatus().name();
        media.setApprovalStatus(targetStatus);
        if (targetStatus == ApprovalStatus.QUARANTINED) {
            media.setMalwareScanStatus("INFECTED");
        }
        MediaAssetEntity saved = mediaAssetRepository.save(media);

        // Transactionally unpublish dependent PUBLISHED lessons
        List<SegmentEntity> segments = segmentRepository.findByMediaAssetId(mediaAssetId);
        Set<UUID> lessonIds = new HashSet<>();
        for (SegmentEntity seg : segments) {
            lessonIds.add(seg.getLessonId());
        }

        for (UUID lessonId : lessonIds) {
            lessonRepository.findById(lessonId).ifPresent(lesson -> {
                if (lesson.getPublicationState() == PublicationState.PUBLISHED) {
                    String lessonBeforeState = lesson.getPublicationState().name();
                    Long lessonBeforeVersion = lesson.getVersion();
                    lesson.setPublicationState(PublicationState.UNPUBLISHED);
                    lesson.setUpdatedByUserId(actorUserId);
                    lessonRepository.save(lesson);
                    auditService.record("LESSON_WITHDRAWAL_MEDIA_INVALIDATED", actorUserId, "LESSON", lesson.getLessonId(),
                            "SUCCESS", "MEDIA_INVALIDATED", lessonBeforeVersion, lesson.getVersion(), lessonBeforeState,
                            lesson.getPublicationState().name(), null);
                }
            });
        }

        auditService.record(eventType, actorUserId, "MEDIA_ASSET", saved.getMediaAssetId(), "SUCCESS", null,
                expectedVersion, saved.getVersion(), beforeState, saved.getApprovalStatus().name(), null);
        return saved;
    }
}
