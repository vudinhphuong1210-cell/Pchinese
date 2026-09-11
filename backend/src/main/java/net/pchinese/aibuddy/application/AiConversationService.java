package net.pchinese.aibuddy.application;

import net.pchinese.aibuddy.domain.AiBuddyScenario;
import net.pchinese.aibuddy.domain.AiConversationStatus;
import net.pchinese.aibuddy.domain.AiMessageStatus;
import net.pchinese.aibuddy.persistence.AiConversationEntity;
import net.pchinese.aibuddy.persistence.AiConversationRepository;
import net.pchinese.aibuddy.persistence.AiMessageEntity;
import net.pchinese.aibuddy.persistence.AiMessageRepository;
import net.pchinese.common.error.ApiException;
import net.pchinese.security.crypto.ConversationCryptoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AiConversationService {
    private final AiConversationRepository conversations;
    private final AiMessageRepository messages;
    private final ConversationCryptoService crypto;
    public AiConversationService(AiConversationRepository conversations, AiMessageRepository messages, ConversationCryptoService crypto) {
        this.conversations = conversations; this.messages = messages; this.crypto = crypto;
    }
    @Transactional
    public ConversationView create(UUID userId, AiBuddyScenario scenario, String title) {
        Instant now = Instant.now();
        AiConversationEntity conversation = AiConversationEntity.create(userId, scenario, crypto.newWrappedConversationKey(userId, now), null, now);
        if (title != null && !title.isBlank()) conversation.rename(crypto.encryptForConversation(conversation, title.trim().getBytes(StandardCharsets.UTF_8)), now);
        return view(conversations.save(conversation));
    }
    @Transactional(readOnly = true)
    public Page<ConversationView> list(UUID userId, Pageable pageable) {
        return conversations.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, AiConversationStatus.ACTIVE, pageable).map(this::view);
    }
    @Transactional(readOnly = true)
    public ConversationDetailView read(UUID userId, UUID conversationId) {
        AiConversationEntity conversation = ownedActive(userId, conversationId);
        List<MessageView> projections = messages.findByConversationIdAndStatusInOrderBySequenceNumberAsc(conversationId,
                List.of(AiMessageStatus.COMPLETE, AiMessageStatus.FAILED)).stream().map(message -> messageView(conversation, message)).toList();
        return new ConversationDetailView(view(conversation), projections);
    }
    @Transactional
    public ConversationView rename(UUID userId, UUID conversationId, String title) {
        AiConversationEntity conversation = ownedActive(userId, conversationId);
        conversation.rename(crypto.encryptForConversation(conversation, title.trim().getBytes(StandardCharsets.UTF_8)), Instant.now());
        return view(conversations.save(conversation));
    }
    @Transactional
    public void delete(UUID userId, UUID conversationId) {
        AiConversationEntity conversation = ownedActive(userId, conversationId);
        conversation.markDeleted(Instant.now());
        conversations.save(conversation);
    }
    public AiConversationEntity ownedActive(UUID userId, UUID conversationId) {
        return conversations.findByIdAndUserIdAndStatus(conversationId, userId, AiConversationStatus.ACTIVE).orElseThrow(ApiException::notFound);
    }
    private ConversationView view(AiConversationEntity conversation) {
        String title = conversation.getTitleCiphertext() == null ? null : new String(crypto.decryptForConversation(conversation, conversation.getTitleCiphertext()), StandardCharsets.UTF_8);
        return new ConversationView(conversation.getId(), conversation.getScenario(), title, conversation.getCreatedAt(), conversation.getUpdatedAt());
    }
    private MessageView messageView(AiConversationEntity conversation, AiMessageEntity message) {
        return new MessageView(message.getId(), message.getSequenceNumber(), message.getSender(), message.getStatus(),
                new String(crypto.decryptForConversation(conversation, message.getContentCiphertext()), StandardCharsets.UTF_8),
                message.getVietnameseExplanationCiphertext() == null ? null : new String(crypto.decryptForConversation(conversation, message.getVietnameseExplanationCiphertext()), StandardCharsets.UTF_8),
                message.getSuggestionCiphertext() == null ? null : new String(crypto.decryptForConversation(conversation, message.getSuggestionCiphertext()), StandardCharsets.UTF_8),
                message.getCreatedAt(), message.getCompletedAt());
    }
    public record ConversationView(UUID id, AiBuddyScenario scenario, String title, Instant createdAt, Instant updatedAt) { }
    public record MessageView(UUID id, int sequenceNumber, net.pchinese.aibuddy.domain.AiMessageSender sender,
                              AiMessageStatus status, String content, String vietnameseExplanation, String suggestion,
                              Instant createdAt, Instant completedAt) { }
    public record ConversationDetailView(ConversationView conversation, List<MessageView> messages) { }
}
