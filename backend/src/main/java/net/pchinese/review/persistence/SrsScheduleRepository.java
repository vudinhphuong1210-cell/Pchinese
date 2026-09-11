package net.pchinese.review.persistence;

import jakarta.persistence.LockModeType;
import net.pchinese.review.domain.SrsStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SrsScheduleRepository extends JpaRepository<SrsScheduleEntity, UUID> {

    Optional<SrsScheduleEntity> findBySavedWordId(UUID savedWordId);

    Optional<SrsScheduleEntity> findBySrsScheduleIdAndUserId(UUID srsScheduleId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SrsScheduleEntity s WHERE s.srsScheduleId = :srsScheduleId AND s.userId = :userId")
    Optional<SrsScheduleEntity> findBySrsScheduleIdAndUserIdForUpdate(
            @Param("srsScheduleId") UUID srsScheduleId,
            @Param("userId") UUID userId
    );

    @Query("SELECT s FROM SrsScheduleEntity s WHERE s.userId = :userId AND s.status != 'SUSPENDED' AND s.dueAt <= :now ORDER BY s.dueAt ASC, s.srsScheduleId ASC")
    List<SrsScheduleEntity> findDueSchedules(
            @Param("userId") UUID userId,
            @Param("now") Instant now,
            Pageable pageable
    );
}
