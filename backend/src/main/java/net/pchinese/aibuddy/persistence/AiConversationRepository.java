package net.pchinese.aibuddy.persistence;

import net.pchinese.aibuddy.domain.AiConversationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AiConversationRepository extends JpaRepository<AiConversationEntity, UUID> {
    Page<AiConversationEntity> findByUserIdAndStatusOrderByUpdatedAtDesc(UUID userId, AiConversationStatus status, Pageable pageable);
    Optional<AiConversationEntity> findByIdAndUserIdAndStatus(UUID id, UUID userId, AiConversationStatus status);
}
