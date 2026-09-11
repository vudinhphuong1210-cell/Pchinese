package net.pchinese.media;

import net.pchinese.content.api.StateConflictException;
import net.pchinese.content.application.ContentAuditService;
import net.pchinese.content.domain.PublicationState;
import net.pchinese.content.persistence.LessonEntity;
import net.pchinese.content.persistence.LessonRepository;
import net.pchinese.content.persistence.SegmentRepository;
import net.pchinese.media.application.MediaLifecycleService;
import net.pchinese.media.domain.ApprovalStatus;
import net.pchinese.media.persistence.MediaAssetEntity;
import net.pchinese.media.persistence.MediaAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaLifecycleServiceTest {
    @Mock private MediaAssetRepository mediaAssetRepository;
    @Mock private SegmentRepository segmentRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private ContentAuditService auditService;
    private MediaLifecycleService service;
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new MediaLifecycleService(mediaAssetRepository, segmentRepository, lessonRepository, auditService);
    }

    @Test
    void approve_requiresCleanPendingMedia() {
        UUID mediaId = UUID.randomUUID();
        MediaAssetEntity media = media(mediaId, ApprovalStatus.PENDING_SCAN, "PENDING", 3L);
        when(mediaAssetRepository.findById(mediaId)).thenReturn(Optional.of(media));

        assertThrows(StateConflictException.class, () -> service.approveMedia(mediaId, 3L, actorId));
        verify(auditService).recordRejected(eq("MEDIA_APPROVE"), eq(actorId), eq("MEDIA_ASSET"), eq(mediaId),
                eq("MEDIA_SCAN_NOT_CLEAN"), eq(3L), eq(3L), eq("PENDING_SCAN"), isNull());
    }

    @Test
    void approve_rejectedMedia_isTerminal() {
        UUID mediaId = UUID.randomUUID();
        MediaAssetEntity media = media(mediaId, ApprovalStatus.REJECTED, "CLEAN", 3L);
        when(mediaAssetRepository.findById(mediaId)).thenReturn(Optional.of(media));

        assertThrows(StateConflictException.class, () -> service.approveMedia(mediaId, 3L, actorId));
        verify(mediaAssetRepository, never()).save(any());
    }

    @Test
    void approve_cleanPendingMedia_succeeds() {
        UUID mediaId = UUID.randomUUID();
        MediaAssetEntity media = media(mediaId, ApprovalStatus.PENDING_SCAN, "CLEAN", 3L);
        when(mediaAssetRepository.findById(mediaId)).thenReturn(Optional.of(media));
        when(mediaAssetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MediaAssetEntity approved = service.approveMedia(mediaId, 3L, actorId);

        assertEquals(ApprovalStatus.APPROVED, approved.getApprovalStatus());
        verify(auditService).record(eq("MEDIA_APPROVE"), eq(actorId), eq("MEDIA_ASSET"), eq(mediaId),
                eq("SUCCESS"), isNull(), eq(3L), eq(3L), eq("PENDING_SCAN"), eq("APPROVED"), isNull());
    }

    @Test
    void quarantine_terminalMedia_isRejectedWithoutAnotherMutation() {
        UUID mediaId = UUID.randomUUID();
        MediaAssetEntity media = media(mediaId, ApprovalStatus.QUARANTINED, "INFECTED", 5L);
        when(mediaAssetRepository.findById(mediaId)).thenReturn(Optional.of(media));

        assertThrows(StateConflictException.class, () -> service.quarantineMedia(mediaId, 5L, actorId));
        verify(mediaAssetRepository, never()).save(any());
    }

    @Test
    void rejectedApprovedMedia_withdrawsPublishedDependentLesson() {
        UUID mediaId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        MediaAssetEntity media = media(mediaId, ApprovalStatus.APPROVED, "CLEAN", 2L);
        var segment = new net.pchinese.content.persistence.SegmentEntity();
        segment.setLessonId(lessonId);
        LessonEntity lesson = new LessonEntity();
        lesson.setLessonId(lessonId); lesson.setPublicationState(PublicationState.PUBLISHED); lesson.setVersion(7L);
        when(mediaAssetRepository.findById(mediaId)).thenReturn(Optional.of(media));
        when(mediaAssetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(segmentRepository.findByMediaAssetId(mediaId)).thenReturn(List.of(segment));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.rejectMedia(mediaId, 2L, actorId);

        assertEquals(ApprovalStatus.REJECTED, media.getApprovalStatus());
        assertEquals(PublicationState.UNPUBLISHED, lesson.getPublicationState());
    }

    private MediaAssetEntity media(UUID id, ApprovalStatus approvalStatus, String scanStatus, Long version) {
        MediaAssetEntity media = new MediaAssetEntity();
        media.setMediaAssetId(id); media.setApprovalStatus(approvalStatus); media.setMalwareScanStatus(scanStatus); media.setVersion(version);
        return media;
    }
}
