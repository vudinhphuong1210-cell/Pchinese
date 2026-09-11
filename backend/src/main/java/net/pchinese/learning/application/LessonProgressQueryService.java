package net.pchinese.learning.application;

import net.pchinese.common.error.ApiException;
import net.pchinese.content.domain.PublicationState;
import net.pchinese.content.persistence.LessonEntity;
import net.pchinese.content.persistence.LessonRepository;
import net.pchinese.content.persistence.SegmentEntity;
import net.pchinese.content.persistence.SegmentRepository;
import net.pchinese.learning.api.PlaybackDtos;
import net.pchinese.learning.domain.LessonProgressStatus;
import net.pchinese.learning.persistence.LessonProgressEntity;
import net.pchinese.learning.persistence.LessonProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class LessonProgressQueryService {

    private final LessonProgressRepository progressRepo;
    private final LessonRepository lessons;
    private final SegmentRepository segments;

    public LessonProgressQueryService(LessonProgressRepository progressRepo,
                                      LessonRepository lessons,
                                      SegmentRepository segments) {
        this.progressRepo = progressRepo;
        this.lessons = lessons;
        this.segments = segments;
    }

    @Transactional(readOnly = true)
    public PlaybackDtos.ProgressProjection getProgress(UUID userId, UUID lessonId) {
        return progressRepo.findByUserIdAndLessonId(userId, lessonId)
                .map(p -> new PlaybackDtos.ProgressProjection(
                        p.getLessonId(),
                        p.getStatus(),
                        p.getCurrentSegmentId(),
                        p.getCompletedSegmentCount(),
                        p.getTotalSegmentCount(),
                        p.getCompletionPercent(),
                        p.getPracticeSeconds(),
                        p.getDictationBestScore(),
                        p.getShadowingBestScore()
                ))
                .orElseGet(() -> {
                    LessonEntity lesson = lessons.findById(lessonId)
                            .filter(l -> l.getPublicationState() == PublicationState.PUBLISHED)
                            .orElseThrow(ApiException::notFound);

                    List<SegmentEntity> segs = segments.findByLessonIdAndPublicationStateOrderBySequenceNoAsc(
                            lessonId, PublicationState.PUBLISHED
                    );
                    UUID firstSegmentId = segs.isEmpty() ? null : segs.get(0).getSegmentId();

                    return new PlaybackDtos.ProgressProjection(
                            lesson.getLessonId(),
                            LessonProgressStatus.NOT_STARTED,
                            firstSegmentId,
                            0,
                            segs.size(),
                            BigDecimal.ZERO,
                            0,
                            null,
                            null
                    );
                });
    }
}
