package net.pchinese.progress.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DailyCheckInRepository extends JpaRepository<DailyCheckInEntity, UUID> {
    boolean existsByUserIdAndCheckInDate(UUID userId, LocalDate checkInDate);
    List<DailyCheckInEntity> findAllByUserIdOrderByCheckInDateAsc(UUID userId);
}
