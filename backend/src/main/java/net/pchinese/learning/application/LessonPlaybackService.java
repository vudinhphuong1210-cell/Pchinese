package net.pchinese.learning.application;

import net.pchinese.common.error.ApiException;
import net.pchinese.content.domain.AccessLevel;
import net.pchinese.content.domain.PublicationState;
import net.pchinese.content.persistence.LessonEntity;
import net.pchinese.content.persistence.LessonRepository;
import net.pchinese.content.persistence.SegmentEntity;
import net.pchinese.content.persistence.SegmentRepository;
import net.pchinese.entitlement.application.EntitlementService;
import net.pchinese.learning.api.PlaybackDtos;
import net.pchinese.learning.domain.LessonProgressStatus;
import net.pchinese.learning.domain.SegmentPlaybackState;
import net.pchinese.learning.persistence.LessonProgressEntity;
import net.pchinese.learning.persistence.LessonProgressRepository;
import net.pchinese.media.persistence.MediaAssetEntity;
import net.pchinese.media.persistence.MediaAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LessonPlaybackService {

    private final LessonRepository lessons;
    private final SegmentRepository segments;
    private final MediaAssetRepository mediaAssets;
    private final LessonProgressRepository progressRepo;
    private final EntitlementService entitlementService;
    private final PlaybackCapabilityService capabilityService;

    public LessonPlaybackService(LessonRepository lessons,
                                 SegmentRepository segments,
                                 MediaAssetRepository mediaAssets,
                                 LessonProgressRepository progressRepo,
                                 EntitlementService entitlementService,
                                 PlaybackCapabilityService capabilityService) {
        this.lessons = lessons;
        this.segments = segments;
        this.mediaAssets = mediaAssets;
        this.progressRepo = progressRepo;
        this.entitlementService = entitlementService;
        this.capabilityService = capabilityService;
    }

    @Transactional
    public PlaybackDtos.PlaybackProjection getPlayback(UUID userId, UUID lessonId) {
        LessonEntity lesson = lessons.findById(lessonId)
                .filter(l -> l.getPublicationState() == PublicationState.PUBLISHED)
                .orElseThrow(ApiException::notFound);

        if (lesson.getAccessLevel() == AccessLevel.PREMIUM) {
            var summary = entitlementService.getSafeEntitlementSummary(userId);
            if (!"PREMIUM".equalsIgnoreCase(summary.planCode())) {
                throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "ENTITLEMENT_REQUIRED", "Premium entitlement is required for this lesson.");
            }
        }

        List<SegmentEntity> publishedSegments = segments
                .findByLessonIdAndPublicationStateOrderBySequenceNoAsc(lessonId, PublicationState.PUBLISHED);

        if (publishedSegments.isEmpty()) {
            throw ApiException.notFound();
        }

        LessonProgressEntity progress = progressRepo.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> {
                    UUID firstSegmentId = publishedSegments.get(0).getSegmentId();
                    LessonProgressEntity initial = LessonProgressEntity.initialize(userId, lessonId, firstSegmentId, publishedSegments.size());
                    return progressRepo.save(initial);
                });

        Map<UUID, MediaAssetEntity> mediaMap = mediaAssets.findAllById(
                publishedSegments.stream().map(SegmentEntity::getMediaAssetId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(MediaAssetEntity::getMediaAssetId, m -> m));

        List<PlaybackDtos.SegmentPlayback> segmentDtos = new ArrayList<>();
        for (SegmentEntity seg : publishedSegments) {
            SegmentPlaybackState state;
            if (progress.getStatus() == LessonProgressStatus.COMPLETED) {
                state = SegmentPlaybackState.COMPLETED;
            } else if (seg.getSegmentId().equals(progress.getCurrentSegmentId())) {
                state = SegmentPlaybackState.CURRENT;
            } else if (seg.getSequenceNo() <= progress.getCompletedSegmentCount()) {
                state = SegmentPlaybackState.COMPLETED;
            } else {
                state = SegmentPlaybackState.LOCKED;
            }

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("startMilliseconds", seg.getStartMilliseconds());
            metadata.put("endMilliseconds", seg.getEndMilliseconds());
            metadata.put("transcriptHanzi", seg.getTranscriptHanzi());
            metadata.put("transcriptPinyin", seg.getTranscriptPinyin());
            metadata.put("translationVi", seg.getTranslationVi());
            metadata.put("dictationHint", seg.getDictationHint());
            metadata.put("segmentType", seg.getSegmentType());

            MediaAssetEntity media = mediaMap.get(seg.getMediaAssetId());
            if (media != null) {
                metadata.put("providerName", media.getProviderName());
                metadata.put("providerAssetIdentifier", media.getProviderAssetIdentifier());
                metadata.put("durationMilliseconds", media.getDurationMilliseconds());
            }

            segmentDtos.add(new PlaybackDtos.SegmentPlayback(seg.getSegmentId(), seg.getSequenceNo(), state, metadata));
        }

        PlaybackCapabilityService.CapabilityResult capability = capabilityService.issueCapability(
                userId, lessonId, progress.getCurrentSegmentId()
        );

        PlaybackDtos.ProgressProjection progressDto = new PlaybackDtos.ProgressProjection(
                progress.getLessonId(),
                progress.getStatus(),
                progress.getCurrentSegmentId(),
                progress.getCompletedSegmentCount(),
                progress.getTotalSegmentCount(),
                progress.getCompletionPercent(),
                progress.getPracticeSeconds(),
                progress.getDictationBestScore(),
                progress.getShadowingBestScore()
        );

        return new PlaybackDtos.PlaybackProjection(
                lessonId,
                capability.token(),
                capability.expiresAt(),
                segmentDtos,
                progressDto
        );
    }
}
