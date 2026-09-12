package net.pchinese.shadowing.application;

import net.pchinese.allowance.application.AiAllowanceCommands;
import net.pchinese.allowance.application.AiAllowanceService;
import net.pchinese.allowance.domain.AllowanceFeatureType;
import net.pchinese.common.api.CorrelationId;
import net.pchinese.common.error.ApiException;
import net.pchinese.content.domain.PublicationState;
import net.pchinese.content.persistence.SegmentEntity;
import net.pchinese.content.persistence.SegmentRepository;
import net.pchinese.learning.application.LessonProgressService;
import net.pchinese.security.SensitiveValueService;
import net.pchinese.shadowing.api.ShadowingDtos;
import net.pchinese.shadowing.domain.RecordingStatus;
import net.pchinese.shadowing.infrastructure.ShadowingAiClient;
import net.pchinese.shadowing.persistence.RecordingEntity;
import net.pchinese.shadowing.persistence.RecordingRepository;
import net.pchinese.shadowing.persistence.ShadowingAttemptEntity;
import net.pchinese.shadowing.persistence.ShadowingAttemptRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class ShadowingService {

    private final RecordingRepository recordingRepo;
    private final ShadowingAttemptRepository attemptRepo;
    private final SegmentRepository segmentRepo;
    private final AiAllowanceService allowanceService;
    private final ShadowingAiClient aiClient;
    private final SensitiveValueService sensitiveValueService;
    private final LessonProgressService progressService;

    public ShadowingService(RecordingRepository recordingRepo,
                            ShadowingAttemptRepository attemptRepo,
                            SegmentRepository segmentRepo,
                            AiAllowanceService allowanceService,
                            ShadowingAiClient aiClient,
                            SensitiveValueService sensitiveValueService,
                            LessonProgressService progressService) {
        this.recordingRepo = recordingRepo;
        this.attemptRepo = attemptRepo;
        this.segmentRepo = segmentRepo;
        this.allowanceService = allowanceService;
        this.aiClient = aiClient;
        this.sensitiveValueService = sensitiveValueService;
        this.progressService = progressService;
    }

    @Transactional
    public ShadowingDtos.RecordingView uploadRecording(UUID userId, UUID segmentId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.validation("Audio recording file is required.");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw ApiException.validation("Audio file size cannot exceed 10MB.");
        }

        segmentRepo.findById(segmentId)
                .filter(s -> s.getPublicationState() == PublicationState.PUBLISHED)
                .orElseThrow(ApiException::notFound);

        try {
            byte[] bytes = file.getBytes();
            String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            byte[] encryptedObjectKey = sensitiveValueService.encrypt("rec:" + userId + ":" + segmentId + ":" + sha256);

            RecordingEntity entity = RecordingEntity.create(
                    userId,
                    file.getContentType() != null ? file.getContentType() : "audio/webm",
                    file.getSize(),
                    sha256,
                    null,
                    encryptedObjectKey
            );
            RecordingEntity saved = recordingRepo.save(entity);

            return new ShadowingDtos.RecordingView(
                    saved.getRecordingId(),
                    saved.getStatus(),
                    saved.getMimeType(),
                    saved.getByteSize(),
                    saved.getDurationMilliseconds(),
                    saved.getExpiresAt()
            );
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to process audio recording upload", e);
        }
    }

    @Transactional
    public ShadowingDtos.ShadowingAttemptView createAttempt(UUID userId, ShadowingDtos.CreateAttemptRequest request, UUID segmentId) {
        RecordingEntity recording = recordingRepo.findByRecordingIdAndUserId(request.recordingId(), userId)
                .orElseThrow(ApiException::notFound);

        if (recording.getStatus() != RecordingStatus.AVAILABLE) {
            throw ApiException.conflict("Recording is not available for assessment.");
        }

        var existingAttempt = attemptRepo.findByRecordingId(request.recordingId());
        if (existingAttempt.isPresent()) {
            return toDto(existingAttempt.get());
        }

        SegmentEntity segment = segmentRepo.findById(segmentId)
                .filter(s -> s.getPublicationState() == PublicationState.PUBLISHED)
                .orElseThrow(ApiException::notFound);

        UUID clientReqId = request.clientRequestId() != null ? request.clientRequestId() : UUID.randomUUID();
        var reservation = allowanceService.reserveOrReuse(new AiAllowanceCommands.ReserveAllowanceCommand(
                userId, AllowanceFeatureType.SHADOWING_ASSESSMENT, "shadow:" + segment.getSegmentId() + ":" + request.recordingId(), clientReqId
        ));

        ShadowingAttemptEntity attempt = ShadowingAttemptEntity.create(
                userId, segment.getLessonId(), segment.getSegmentId(), recording.getRecordingId(), reservation.eventId()
        );
        attempt = attemptRepo.save(attempt);

        try {
            long dispatchStartedAt = System.nanoTime();
            String corrId = CorrelationId.current();
            UUID correlationUuid = corrId != null ? UUID.fromString(corrId) : UUID.randomUUID();

            ShadowingAiClient.ShadowingAssessResponse response = aiClient.assess(new ShadowingAiClient.ShadowingAssessRequest(
                    correlationUuid,
                    clientReqId,
                    segment.getSegmentId(),
                    segment.getTranscriptHanzi(),
                    segment.getTranscriptPinyin(),
                    recording.getRecordingId(),
                    ""
            ));

            Instant now = Instant.now();
            byte[] encFeedback = sensitiveValueService.encrypt(response.feedback());
            attempt.succeed(
                    response.overallScore(),
                    response.pronunciationScore(),
                    response.toneScore(),
                    response.rhythmScore(),
                    encFeedback,
                    now
            );
            recording.markSucceeded(now);
            recordingRepo.save(recording);

            long durationMs = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - dispatchStartedAt);
            allowanceService.succeed(reservation.eventId(), correlationUuid.toString(), response.telemetry(), durationMs);
            progressService.updatePracticeMetrics(userId, segment.getLessonId(), null, response.overallScore());

            return toDto(attemptRepo.save(attempt));
        } catch (Exception e) {
            attempt.fail(Instant.now());
            attemptRepo.save(attempt);
            allowanceService.refundOnce(reservation.eventId(), "PROVIDER_ERROR");
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public ShadowingDtos.ShadowingAttemptView getAttempt(UUID userId, UUID attemptId) {
        ShadowingAttemptEntity attempt = attemptRepo.findByShadowingAttemptIdAndUserId(attemptId, userId)
                .orElseThrow(ApiException::notFound);
        return toDto(attempt);
    }

    @Transactional(readOnly = true)
    public Page<ShadowingDtos.ShadowingAttemptView> listAttempts(UUID userId, UUID lessonId, UUID segmentId, Pageable pageable) {
        Page<ShadowingAttemptEntity> page;
        if (lessonId != null && segmentId != null) {
            page = attemptRepo.findByUserIdAndLessonIdAndSegmentIdOrderByCreatedAtDesc(userId, lessonId, segmentId, pageable);
        } else if (lessonId != null) {
            page = attemptRepo.findByUserIdAndLessonIdOrderByCreatedAtDesc(userId, lessonId, pageable);
        } else {
            page = attemptRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return page.map(this::toDto);
    }

    private ShadowingDtos.ShadowingAttemptView toDto(ShadowingAttemptEntity entity) {
        String feedback = null;
        if (entity.getFeedbackCiphertext() != null) {
            try {
                feedback = sensitiveValueService.decryptToString(entity.getFeedbackCiphertext());
            } catch (Exception ignored) {}
        }

        return new ShadowingDtos.ShadowingAttemptView(
                entity.getShadowingAttemptId(),
                entity.getLessonId(),
                entity.getSegmentId(),
                entity.getRecordingId(),
                entity.getStatus(),
                entity.getOverallScore(),
                entity.getPronunciationScore(),
                entity.getToneScore(),
                entity.getRhythmScore(),
                feedback,
                entity.getSubmittedAt(),
                entity.getEvaluatedAt()
        );
    }
}
