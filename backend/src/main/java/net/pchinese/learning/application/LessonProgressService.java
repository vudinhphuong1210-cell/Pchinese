package net.pchinese.learning.application;

import net.pchinese.common.error.ApiException;
import net.pchinese.content.domain.PublicationState;
import net.pchinese.content.persistence.SegmentEntity;
import net.pchinese.content.persistence.SegmentRepository;
import net.pchinese.learning.api.PlaybackDtos;
import net.pchinese.learning.domain.LessonProgressStatus;
import net.pchinese.learning.domain.PlaybackEventType;
import net.pchinese.learning.persistence.LessonProgressEntity;
import net.pchinese.learning.persistence.LessonProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class LessonProgressService {

    private final LessonProgressRepository progressRepo;
    private final SegmentRepository segments;
    private final PlaybackCapabilityService capabilityService;

    public LessonProgressService(LessonProgressRepository progressRepo,
                                 SegmentRepository segments,
                                 PlaybackCapabilityService capabilityService) {
        this.progressRepo = progressRepo;
        this.segments = segments;
        this.capabilityService = capabilityService;
    }

    @Transactional
    public PlaybackDtos.ProgressProjection submitPlaybackEvent(UUID userId, UUID lessonId, PlaybackDtos.PlaybackEventRequest request) {
        capabilityService.validateCapability(
                request.playbackCapability(), userId, lessonId, request.segmentId()
        );

        LessonProgressEntity progress = progressRepo.findByUserIdAndLessonId(userId, lessonId)
                .orElseThrow(ApiException::notFound);

        if (progress.getStatus() == LessonProgressStatus.COMPLETED) {
            return toDto(progress);
        }

        List<SegmentEntity> publishedSegments = segments
                .findByLessonIdAndPublicationStateOrderBySequenceNoAsc(lessonId, PublicationState.PUBLISHED);

        int currentIdx = -1;
        for (int i = 0; i < publishedSegments.size(); i++) {
            if (publishedSegments.get(i).getSegmentId().equals(request.segmentId())) {
                currentIdx = i;
                break;
            }
        }

        if (currentIdx < 0) {
            throw ApiException.notFound();
        }

        SegmentEntity targetSegment = publishedSegments.get(currentIdx);

        // Idempotent check: if already completed previously
        if (targetSegment.getSequenceNo() <= progress.getCompletedSegmentCount()) {
            return toDto(progress);
        }

        // Must match active current segment
        if (progress.getCurrentSegmentId() != null && !progress.getCurrentSegmentId().equals(request.segmentId())) {
            throw ApiException.conflict("Segment is not active or locked.");
        }

        Instant now = Instant.now();
        if (request.event() == PlaybackEventType.PROGRESS) {
            int playedSeconds = Math.max(1, request.positionMs() / 1000);
            progress.advanceWatermark(playedSeconds, now);
        } else if (request.event() == PlaybackEventType.ENDED) {
            UUID nextSegmentId = (currentIdx + 1 < publishedSegments.size())
                    ? publishedSegments.get(currentIdx + 1).getSegmentId()
                    : null;
            progress.completeSegment(nextSegmentId, now);
        }

        LessonProgressEntity saved = progressRepo.save(progress);
        return toDto(saved);
    }

    @Transactional
    public void updatePracticeMetrics(UUID userId, UUID lessonId, BigDecimal dictationScore, BigDecimal shadowingScore) {
        LessonProgressEntity progress = progressRepo.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> {
                    List<SegmentEntity> published = segments.findByLessonIdAndPublicationStateOrderBySequenceNoAsc(
                            lessonId, PublicationState.PUBLISHED
                    );
                    UUID firstSegment = published.isEmpty() ? null : published.get(0).getSegmentId();
                    return progressRepo.save(LessonProgressEntity.initialize(userId, lessonId, firstSegment, published.size()));
                });

        Instant now = Instant.now();
        if (dictationScore != null) {
            progress.updateDictationScore(dictationScore, now);
        }
        if (shadowingScore != null) {
            progress.updateShadowingScore(shadowingScore, now);
        }
        progressRepo.save(progress);
    }

    public PlaybackDtos.ProgressProjection toDto(LessonProgressEntity progress) {
        return new PlaybackDtos.ProgressProjection(
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
    }
}
