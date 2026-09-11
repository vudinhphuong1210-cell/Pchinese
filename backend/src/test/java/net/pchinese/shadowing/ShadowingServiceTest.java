package net.pchinese.shadowing;

import net.pchinese.allowance.application.AiAllowanceCommands;
import net.pchinese.allowance.application.AiAllowanceService;
import net.pchinese.allowance.domain.AllowanceEventStatus;
import net.pchinese.common.error.ApiException;
import net.pchinese.content.domain.PublicationState;
import net.pchinese.content.persistence.SegmentEntity;
import net.pchinese.content.persistence.SegmentRepository;
import net.pchinese.learning.application.LessonProgressService;
import net.pchinese.security.SensitiveValueService;
import net.pchinese.shadowing.api.ShadowingDtos;
import net.pchinese.shadowing.application.ShadowingService;
import net.pchinese.shadowing.domain.RecordingStatus;
import net.pchinese.shadowing.domain.ShadowingAttemptStatus;
import net.pchinese.shadowing.infrastructure.ShadowingAiClient;
import net.pchinese.shadowing.persistence.RecordingEntity;
import net.pchinese.shadowing.persistence.RecordingRepository;
import net.pchinese.shadowing.persistence.ShadowingAttemptEntity;
import net.pchinese.shadowing.persistence.ShadowingAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShadowingServiceTest {

    @Mock
    private RecordingRepository recordingRepo;
    @Mock
    private ShadowingAttemptRepository attemptRepo;
    @Mock
    private SegmentRepository segmentRepo;
    @Mock
    private AiAllowanceService allowanceService;
    @Mock
    private ShadowingAiClient aiClient;
    @Mock
    private SensitiveValueService sensitiveValueService;
    @Mock
    private LessonProgressService progressService;

    private ShadowingService shadowingService;

    @BeforeEach
    void setUp() {
        shadowingService = new ShadowingService(
                recordingRepo,
                attemptRepo,
                segmentRepo,
                allowanceService,
                aiClient,
                sensitiveValueService,
                progressService
        );
    }

    @Test
    void uploadRecording_RejectsEmptyFile() {
        UUID userId = UUID.randomUUID();
        UUID segmentId = UUID.randomUUID();
        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

        assertThrows(ApiException.class, () -> shadowingService.uploadRecording(userId, segmentId, emptyFile));
    }

    @Test
    void uploadRecording_SavesEntityAndCalculatesHash() {
        UUID userId = UUID.randomUUID();
        UUID segmentId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "audio.webm", "audio/webm", "test audio content".getBytes());

        SegmentEntity segment = mock(SegmentEntity.class);
        when(segment.getPublicationState()).thenReturn(PublicationState.PUBLISHED);
        when(segmentRepo.findById(segmentId)).thenReturn(Optional.of(segment));
        when(sensitiveValueService.encrypt(anyString())).thenReturn(new byte[]{1, 2, 3});
        when(recordingRepo.save(any(RecordingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShadowingDtos.RecordingView result = shadowingService.uploadRecording(userId, segmentId, file);

        assertNotNull(result);
        assertEquals(RecordingStatus.AVAILABLE, result.status());
        assertEquals("audio/webm", result.mimeType());
        verify(recordingRepo).save(any(RecordingEntity.class));
    }

    @Test
    void createAttempt_SuccessfulAssessment_UpdatesMetricsAndEncryptsFeedback() {
        UUID userId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID segmentId = UUID.randomUUID();
        UUID recordingId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        RecordingEntity recording = RecordingEntity.create(userId, "audio/webm", 1000L, "hash", null, new byte[]{1});

        SegmentEntity segment = mock(SegmentEntity.class);
        when(segment.getSegmentId()).thenReturn(segmentId);
        when(segment.getLessonId()).thenReturn(lessonId);
        when(segment.getTranscriptHanzi()).thenReturn("你好");
        when(segment.getTranscriptPinyin()).thenReturn("nǐ hǎo");
        when(segment.getPublicationState()).thenReturn(PublicationState.PUBLISHED);

        when(recordingRepo.findByRecordingIdAndUserId(recordingId, userId)).thenReturn(Optional.of(recording));
        when(attemptRepo.findByRecordingId(recordingId)).thenReturn(Optional.empty());
        when(segmentRepo.findById(segmentId)).thenReturn(Optional.of(segment));
        when(allowanceService.reserveOrReuse(any(AiAllowanceCommands.ReserveAllowanceCommand.class)))
                .thenReturn(new AiAllowanceCommands.AllowanceReservationResult(eventId, AllowanceEventStatus.RESERVED, false, 29));

        when(aiClient.assess(any(ShadowingAiClient.ShadowingAssessRequest.class)))
                .thenReturn(new ShadowingAiClient.ShadowingAssessResponse(
                        UUID.randomUUID(),
                        BigDecimal.valueOf(90),
                        BigDecimal.valueOf(92),
                        BigDecimal.valueOf(88),
                        BigDecimal.valueOf(90),
                        "Rất tốt!"
                ));
        when(sensitiveValueService.encrypt(anyString())).thenReturn(new byte[]{4, 5, 6});
        when(attemptRepo.save(any(ShadowingAttemptEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ShadowingDtos.CreateAttemptRequest request = new ShadowingDtos.CreateAttemptRequest(recordingId, UUID.randomUUID());
        ShadowingDtos.ShadowingAttemptView view = shadowingService.createAttempt(userId, request, segmentId);

        assertNotNull(view);
        assertEquals(ShadowingAttemptStatus.EVALUATED, view.status());
        assertEquals(BigDecimal.valueOf(90), view.overallScore());
        verify(allowanceService).succeed(eq(eventId), anyString());
        verify(progressService).updatePracticeMetrics(userId, lessonId, null, BigDecimal.valueOf(90));
    }

    @Test
    void createAttempt_AiFailure_MarksFailedAndRefundsQuota() {
        UUID userId = UUID.randomUUID();
        UUID segmentId = UUID.randomUUID();
        UUID recordingId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        RecordingEntity recording = RecordingEntity.create(userId, "audio/webm", 1000L, "hash", null, new byte[]{1});

        SegmentEntity segment = mock(SegmentEntity.class);
        when(segment.getSegmentId()).thenReturn(segmentId);
        when(segment.getTranscriptHanzi()).thenReturn("你好");
        when(segment.getPublicationState()).thenReturn(PublicationState.PUBLISHED);

        when(recordingRepo.findByRecordingIdAndUserId(recordingId, userId)).thenReturn(Optional.of(recording));
        when(attemptRepo.findByRecordingId(recordingId)).thenReturn(Optional.empty());
        when(segmentRepo.findById(segmentId)).thenReturn(Optional.of(segment));
        when(allowanceService.reserveOrReuse(any(AiAllowanceCommands.ReserveAllowanceCommand.class)))
                .thenReturn(new AiAllowanceCommands.AllowanceReservationResult(eventId, AllowanceEventStatus.RESERVED, false, 29));
        when(attemptRepo.save(any(ShadowingAttemptEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        when(aiClient.assess(any(ShadowingAiClient.ShadowingAssessRequest.class)))
                .thenThrow(new RuntimeException("AI Provider Timeout"));

        ShadowingDtos.CreateAttemptRequest request = new ShadowingDtos.CreateAttemptRequest(recordingId, UUID.randomUUID());

        assertThrows(RuntimeException.class, () -> shadowingService.createAttempt(userId, request, segmentId));
        verify(allowanceService).refundOnce(eventId, "PROVIDER_ERROR");
        verify(attemptRepo, atLeastOnce()).save(any(ShadowingAttemptEntity.class));
    }
}
