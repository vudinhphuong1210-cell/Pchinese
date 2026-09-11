package net.pchinese.dictation.application;

import net.pchinese.common.error.ApiException;
import net.pchinese.content.domain.PublicationState;
import net.pchinese.content.persistence.SegmentEntity;
import net.pchinese.content.persistence.SegmentRepository;
import net.pchinese.dictation.api.DictationDtos;
import net.pchinese.dictation.domain.DictationAttemptStatus;
import net.pchinese.dictation.persistence.DictationAttemptEntity;
import net.pchinese.dictation.persistence.DictationAttemptRepository;
import net.pchinese.learning.application.LessonProgressService;
import net.pchinese.security.SensitiveValueService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class DictationService {

    private final DictationAttemptRepository attemptRepo;
    private final SegmentRepository segmentRepo;
    private final DictationEvaluator evaluator;
    private final SensitiveValueService sensitiveValueService;
    private final LessonProgressService progressService;

    public DictationService(DictationAttemptRepository attemptRepo,
                            SegmentRepository segmentRepo,
                            DictationEvaluator evaluator,
                            SensitiveValueService sensitiveValueService,
                            LessonProgressService progressService) {
        this.attemptRepo = attemptRepo;
        this.segmentRepo = segmentRepo;
        this.evaluator = evaluator;
        this.sensitiveValueService = sensitiveValueService;
        this.progressService = progressService;
    }

    @Transactional
    public DictationDtos.DictationAttemptView startAttempt(UUID userId, DictationDtos.StartAttemptRequest request) {
        SegmentEntity segment = segmentRepo.findById(request.segmentId())
                .filter(s -> s.getPublicationState() == PublicationState.PUBLISHED)
                .orElseThrow(ApiException::notFound);

        DictationAttemptEntity attempt = attemptRepo.findFirstByUserIdAndSegmentIdAndStatusOrderByCreatedAtDesc(
                userId, request.segmentId(), DictationAttemptStatus.IN_PROGRESS
        ).orElseGet(() -> attemptRepo.save(
                DictationAttemptEntity.start(userId, request.lessonId(), request.segmentId())
        ));

        return toDto(attempt);
    }

    @Transactional
    public DictationDtos.DictationAttemptView submitAttempt(UUID userId, UUID attemptId, DictationDtos.SubmitAttemptRequest request) {
        DictationAttemptEntity attempt = attemptRepo.findByDictationAttemptIdAndUserId(attemptId, userId)
                .orElseThrow(ApiException::notFound);

        if (attempt.getStatus() == DictationAttemptStatus.EVALUATED) {
            return toDto(attempt);
        }

        SegmentEntity segment = segmentRepo.findById(attempt.getSegmentId())
                .filter(s -> s.getPublicationState() == PublicationState.PUBLISHED)
                .orElseThrow(ApiException::notFound);

        DictationEvaluator.EvaluationResult eval = evaluator.evaluate(request.answer(), segment.getTranscriptHanzi());

        byte[] encryptedAnswer = sensitiveValueService.encrypt(request.answer());
        byte[] encryptedFeedback = sensitiveValueService.encrypt(eval.feedback());

        attempt.evaluate(encryptedAnswer, eval.score(), encryptedFeedback, Instant.now());
        DictationAttemptEntity saved = attemptRepo.save(attempt);

        progressService.updatePracticeMetrics(userId, attempt.getLessonId(), eval.score(), null);

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public DictationDtos.DictationAttemptView getAttempt(UUID userId, UUID attemptId) {
        DictationAttemptEntity attempt = attemptRepo.findByDictationAttemptIdAndUserId(attemptId, userId)
                .orElseThrow(ApiException::notFound);
        return toDto(attempt);
    }

    @Transactional(readOnly = true)
    public Page<DictationDtos.DictationAttemptView> listAttempts(UUID userId, UUID lessonId, UUID segmentId, Pageable pageable) {
        Page<DictationAttemptEntity> page;
        if (lessonId != null && segmentId != null) {
            page = attemptRepo.findByUserIdAndLessonIdAndSegmentIdOrderByCreatedAtDesc(userId, lessonId, segmentId, pageable);
        } else if (lessonId != null) {
            page = attemptRepo.findByUserIdAndLessonIdOrderByCreatedAtDesc(userId, lessonId, pageable);
        } else {
            page = attemptRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return page.map(this::toDto);
    }

    private DictationDtos.DictationAttemptView toDto(DictationAttemptEntity entity) {
        String feedback = null;
        if (entity.getFeedbackCiphertext() != null) {
            try {
                feedback = sensitiveValueService.decryptToString(entity.getFeedbackCiphertext());
            } catch (Exception ignored) {}
        }

        return new DictationDtos.DictationAttemptView(
                entity.getDictationAttemptId(),
                entity.getLessonId(),
                entity.getSegmentId(),
                entity.getStatus(),
                entity.getOverallScore(),
                entity.getAccuracyPercent(),
                feedback,
                entity.getStartedAt(),
                entity.getSubmittedAt(),
                entity.getEvaluatedAt()
        );
    }
}
