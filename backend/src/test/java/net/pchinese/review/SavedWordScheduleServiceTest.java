package net.pchinese.review;

import net.pchinese.review.application.SavedWordScheduleService;
import net.pchinese.review.domain.SrsStatus;
import net.pchinese.review.persistence.SrsScheduleEntity;
import net.pchinese.review.persistence.SrsScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SavedWordScheduleServiceTest {

    private SrsScheduleRepository scheduleRepository;
    private SavedWordScheduleService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID savedWordId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        scheduleRepository = mock(SrsScheduleRepository.class);
        service = new SavedWordScheduleService(scheduleRepository);
    }

    @Test
    @DisplayName("onSavedWordCreated creates new LEARNING schedule due immediately when none exists")
    void testOnSavedWordCreatedNew() {
        when(scheduleRepository.findBySavedWordId(savedWordId)).thenReturn(Optional.empty());

        service.onSavedWordCreated(userId, savedWordId);

        ArgumentCaptor<SrsScheduleEntity> captor = ArgumentCaptor.forClass(SrsScheduleEntity.class);
        verify(scheduleRepository).save(captor.capture());

        SrsScheduleEntity saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getSavedWordId()).isEqualTo(savedWordId);
        assertThat(saved.getStatus()).isEqualTo(SrsStatus.LEARNING);
        assertThat(saved.getEaseFactor()).isEqualTo(new BigDecimal("2.500"));
        assertThat(saved.getRepetitions()).isEqualTo(0);
    }

    @Test
    @DisplayName("onSavedWordDeleted sets status to SUSPENDED")
    void testOnSavedWordDeleted() {
        SrsScheduleEntity existing = new SrsScheduleEntity(
                UUID.randomUUID(),
                savedWordId,
                userId,
                SrsStatus.LEARNING,
                Instant.now(),
                BigDecimal.ZERO,
                new BigDecimal("2.500"),
                0,
                0,
                null,
                Instant.now(),
                Instant.now(),
                0L
        );
        when(scheduleRepository.findBySavedWordId(savedWordId)).thenReturn(Optional.of(existing));

        service.onSavedWordDeleted(userId, savedWordId);

        verify(scheduleRepository).save(existing);
        assertThat(existing.getStatus()).isEqualTo(SrsStatus.SUSPENDED);
    }

    @Test
    @DisplayName("onSavedWordRestored resumes SUSPENDED schedule without resetting ease or repetitions")
    void testOnSavedWordRestored() {
        SrsScheduleEntity suspended = new SrsScheduleEntity(
                UUID.randomUUID(),
                savedWordId,
                userId,
                SrsStatus.SUSPENDED,
                Instant.now().minusSeconds(3600),
                new BigDecimal("3.000"),
                new BigDecimal("2.350"),
                2,
                0,
                Instant.now().minusSeconds(86400 * 3),
                Instant.now().minusSeconds(86400 * 3),
                Instant.now().minusSeconds(86400 * 3),
                1L
        );
        when(scheduleRepository.findBySavedWordId(savedWordId)).thenReturn(Optional.of(suspended));

        service.onSavedWordRestored(userId, savedWordId);

        verify(scheduleRepository).save(suspended);
        assertThat(suspended.getStatus()).isEqualTo(SrsStatus.REVIEW);
        assertThat(suspended.getEaseFactor()).isEqualTo(new BigDecimal("2.350"));
        assertThat(suspended.getRepetitions()).isEqualTo(2);
    }
}
