package net.pchinese.aibuddy.persistence;

import net.pchinese.aibuddy.domain.AiMessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiMessageRepository extends JpaRepository<AiMessageEntity, UUID> {
    List<AiMessageEntity> findByConversationIdAndStatusOrderBySequenceNumberAsc(UUID conversationId, AiMessageStatus status);
    List<AiMessageEntity> findByConversationIdAndStatusInOrderBySequenceNumberAsc(UUID conversationId, List<AiMessageStatus> statuses);
    Optional<AiMessageEntity> findByUserIdAndClientRequestId(UUID userId, UUID clientRequestId);
    List<AiMessageEntity> findTop10ByConversationIdAndStatusOrderBySequenceNumberDesc(UUID conversationId, AiMessageStatus status);
    Optional<AiMessageEntity> findTopByConversationIdOrderBySequenceNumberDesc(UUID conversationId);
    Optional<AiMessageEntity> findByConversationIdAndSequenceNumber(UUID conversationId, int sequenceNumber);
}
