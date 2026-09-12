package net.pchinese.review.application;

import net.pchinese.review.domain.SrsStatus;
import net.pchinese.review.persistence.SrsScheduleEntity;
import net.pchinese.review.persistence.SrsScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class SavedWordScheduleService implements SrsScheduleCommands {

    private final SrsScheduleRepository scheduleRepository;

    public SavedWordScheduleService(SrsScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    @Transactional
    public void onSavedWordCreated(UUID userId, UUID savedWordId) {
        scheduleRepository.findBySavedWordId(savedWordId).ifPresentOrElse(
                existing -> {
                    if (existing.getStatus() == SrsStatus.SUSPENDED) {
                        onSavedWordRestored(userId, savedWordId);
                    }
                },
                () -> {
                    Instant now = Instant.now();
                    SrsScheduleEntity newSchedule = new SrsScheduleEntity(
                            UUID.randomUUID(),
                            savedWordId,
                            userId,
                            SrsStatus.LEARNING,
                            now,
                            BigDecimal.ZERO,
                            new BigDecimal("2.500"),
                            0,
                            0,
                            null,
                            now,
                            now,
                            null
                    );
                    scheduleRepository.save(newSchedule);
                }
        );
    }

    @Override
    @Transactional
    public void onSavedWordDeleted(UUID userId, UUID savedWordId) {
        scheduleRepository.findBySavedWordId(savedWordId).ifPresent(schedule -> {
            if (schedule.getUserId().equals(userId)) {
                schedule.setStatus(SrsStatus.SUSPENDED);
                schedule.setUpdatedAt(Instant.now());
                scheduleRepository.save(schedule);
            }
        });
    }

    @Override
    @Transactional
    public void onSavedWordRestored(UUID userId, UUID savedWordId) {
        scheduleRepository.findBySavedWordId(savedWordId).ifPresent(schedule -> {
            if (schedule.getUserId().equals(userId) && schedule.getStatus() == SrsStatus.SUSPENDED) {
                SrsStatus restoredStatus = schedule.getRepetitions() > 0 ? SrsStatus.REVIEW : SrsStatus.LEARNING;
                schedule.setStatus(restoredStatus);
                schedule.setUpdatedAt(Instant.now());
                scheduleRepository.save(schedule);
            }
        });
    }
}
