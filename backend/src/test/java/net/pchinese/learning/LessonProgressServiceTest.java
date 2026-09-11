package net.pchinese.learning;

import net.pchinese.content.domain.PublicationState;
import net.pchinese.content.persistence.SegmentEntity;
import net.pchinese.content.persistence.SegmentRepository;
import net.pchinese.learning.api.PlaybackDtos;
import net.pchinese.learning.application.LessonProgressService;
import net.pchinese.learning.application.PlaybackCapabilityService;
import net.pchinese.learning.domain.LessonProgressStatus;
import net.pchinese.learning.domain.PlaybackEventType;
import net.pchinese.learning.persistence.LessonProgressEntity;
import net.pchinese.learning.persistence.LessonProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LessonProgressServiceTest {

    private LessonProgressRepository progressRepo;
    private SegmentRepository segmentRepo;
    private PlaybackCapabilityService capabilityService;
    private LessonProgressService service;

    @BeforeEach
    void setup() {
        progressRepo = mock(LessonProgressRepository.class);
        segmentRepo = mock(SegmentRepository.class);
        capabilityService = mock(PlaybackCapabilityService.class);
        service = new LessonProgressService(progressRepo, segmentRepo, capabilityService);
    }

    @Test
    void advancesWatermarkOnProgressEvent() {
        UUID userId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID segmentId = UUID.randomUUID();

        SegmentEntity seg = mock(SegmentEntity.class);
        when(seg.getSegmentId()).thenReturn(segmentId);
        when(seg.getSequenceNo()).thenReturn(1);
        when(segmentRepo.findByLessonIdAndPublicationStateOrderBySequenceNoAsc(lessonId, PublicationState.PUBLISHED))
                .thenReturn(List.of(seg));

        LessonProgressEntity progress = LessonProgressEntity.initialize(userId, lessonId, segmentId, 1);
        when(progressRepo.findByUserIdAndLessonId(userId, lessonId)).thenReturn(Optional.of(progress));
        when(progressRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PlaybackDtos.PlaybackEventRequest request = new PlaybackDtos.PlaybackEventRequest(
                segmentId, PlaybackEventType.PROGRESS, 5000, UUID.randomUUID(), "valid-token"
        );

        var result = service.submitPlaybackEvent(userId, lessonId, request);
        assertNotNull(result);
        assertEquals(LessonProgressStatus.IN_PROGRESS, result.status());
        verify(capabilityService).validateCapability("valid-token", userId, lessonId, segmentId);
    }

    @Test
    void completesLessonWhenFinalSegmentEnds() {
        UUID userId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        UUID segmentId = UUID.randomUUID();

        SegmentEntity seg = mock(SegmentEntity.class);
        when(seg.getSegmentId()).thenReturn(segmentId);
        when(seg.getSequenceNo()).thenReturn(1);
        when(segmentRepo.findByLessonIdAndPublicationStateOrderBySequenceNoAsc(lessonId, PublicationState.PUBLISHED))
                .thenReturn(List.of(seg));

        LessonProgressEntity progress = LessonProgressEntity.initialize(userId, lessonId, segmentId, 1);
        when(progressRepo.findByUserIdAndLessonId(userId, lessonId)).thenReturn(Optional.of(progress));
        when(progressRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PlaybackDtos.PlaybackEventRequest request = new PlaybackDtos.PlaybackEventRequest(
                segmentId, PlaybackEventType.ENDED, 10000, UUID.randomUUID(), "valid-token"
        );

        var result = service.submitPlaybackEvent(userId, lessonId, request);
        assertNotNull(result);
        assertEquals(LessonProgressStatus.COMPLETED, result.status());
        assertEquals(1, result.completedSegmentCount());
    }
}
