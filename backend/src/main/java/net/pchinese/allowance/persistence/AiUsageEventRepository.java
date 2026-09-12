package net.pchinese.allowance.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface AiUsageEventRepository extends JpaRepository<AiUsageEventEntity, UUID> {
    Optional<AiUsageEventEntity> findByUserIdAndClientRequestId(UUID userId, UUID clientRequestId);
    Optional<AiUsageEventEntity> findByClientRequestId(UUID clientRequestId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from AiUsageEventEntity event where event.aiUsageEventId = :eventId")
    Optional<AiUsageEventEntity> findByIdLocked(UUID eventId);
}
