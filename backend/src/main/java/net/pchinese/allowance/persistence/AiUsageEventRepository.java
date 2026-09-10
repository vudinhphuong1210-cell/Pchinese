package net.pchinese.allowance.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiUsageEventRepository extends JpaRepository<AiUsageEventEntity, UUID> {
    Optional<AiUsageEventEntity> findByUserIdAndClientRequestId(UUID userId, UUID clientRequestId);
    Optional<AiUsageEventEntity> findByClientRequestId(UUID clientRequestId);
}
