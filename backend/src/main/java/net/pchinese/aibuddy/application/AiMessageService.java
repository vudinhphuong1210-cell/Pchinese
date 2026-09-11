package net.pchinese.aibuddy.application;

import net.pchinese.aibuddy.domain.AiMessageStatus;
import net.pchinese.aibuddy.persistence.AiConversationEntity;
import net.pchinese.aibuddy.persistence.AiMessageEntity;
import net.pchinese.aibuddy.persistence.AiMessageRepository;
import net.pchinese.aibuddy.infrastructure.AiBuddyClient;
import net.pchinese.allowance.application.AiAllowanceCommands;
import net.pchinese.allowance.application.AiAllowanceService;
import net.pchinese.allowance.domain.AllowanceFeatureType;
import net.pchinese.common.api.CorrelationId;
import net.pchinese.common.error.ApiException;
import net.pchinese.security.crypto.ConversationCryptoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AiMessageService {
    private final AiConversationService conversations; private final AiMessageRepository messages; private final AiBuddySafetyService safety;
    private final AiAllowanceService allowance; private final AiBuddyClient client; private final ConversationCryptoService crypto; private final TransactionTemplate transactions;
    public AiMessageService(AiConversationService conversations, AiMessageRepository messages, AiBuddySafetyService safety, AiAllowanceService allowance,
                            AiBuddyClient client, ConversationCryptoService crypto, TransactionTemplate transactions) {
        this.conversations = conversations; this.messages = messages; this.safety = safety; this.allowance = allowance; this.client = client; this.crypto = crypto; this.transactions = transactions;
    }
    public MessagePairView send(UUID userId, UUID conversationId, UUID clientRequestId, String content) {
        PreparedRequest prepared = transactions.execute(status -> prepare(userId, conversationId, clientRequestId, content));
        if (prepared.replay() != null) return prepared.replay();
        try {
            AiBuddyClient.AiBuddyResponse response = client.respond(prepared.privateRequest());
            return transactions.execute(status -> succeed(prepared, response));
        } catch (ApiException exception) {
            transactions.executeWithoutResult(status -> fail(prepared, exception.code())); throw exception;
        } catch (RuntimeException exception) {
            transactions.executeWithoutResult(status -> fail(prepared, "SERVICE_UNAVAILABLE"));
            throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "AI service is temporarily unavailable.");
        }
    }
    private PreparedRequest prepare(UUID userId, UUID conversationId, UUID requestId, String content) {
        var existing = messages.findByUserIdAndClientRequestId(userId, requestId);
        if (existing.isPresent()) {
            AiMessageEntity message = existing.get();
            String expected = fingerprint(conversationId, content);
            if (!expected.equals(message.getRequestFingerprint())) throw ApiException.conflict("Client request ID was used with a different message.");
            if (message.getStatus() != AiMessageStatus.COMPLETE) throw ApiException.conflict("The original AI Buddy request did not complete.");
            AiMessageEntity assistant = messages.findByConversationIdAndSequenceNumber(conversationId, message.getSequenceNumber() + 1)
                    .filter(value -> value.getStatus() == AiMessageStatus.COMPLETE)
                    .orElseThrow(() -> ApiException.conflict("Replay result is unavailable."));
            AiConversationEntity conversation = conversations.ownedActive(userId, conversationId);
            return new PreparedRequest(null, null, null, pair(conversation, message, assistant));
        }
        AiConversationEntity conversation = conversations.ownedActive(userId, conversationId);
        safety.checkAndRecord(userId, conversation.getScenario(), content);
        String fingerprint = fingerprint(conversationId, content);
        var reservation = allowance.reserveOrReuse(new AiAllowanceCommands.ReserveAllowanceCommand(userId, AllowanceFeatureType.AI_BUDDY, conversationId + ":" + fingerprint, requestId));
        int nextSequence = messages.findTopByConversationIdOrderBySequenceNumberDesc(conversationId).map(value -> value.getSequenceNumber() + 1).orElse(1);
        Instant now = Instant.now();
        AiMessageEntity learner = AiMessageEntity.learner(conversationId, userId, nextSequence, requestId, fingerprint,
                crypto.encryptForConversation(conversation, content.trim().getBytes(StandardCharsets.UTF_8)), now);
        messages.save(learner);
        List<AiBuddyClient.ContextMessage> context = messages.findTop10ByConversationIdAndStatusOrderBySequenceNumberDesc(conversationId, AiMessageStatus.COMPLETE).stream()
                .sorted(java.util.Comparator.comparingInt(AiMessageEntity::getSequenceNumber))
                .map(value -> new AiBuddyClient.ContextMessage(value.getSender().name(), new String(crypto.decryptForConversation(conversation, value.getContentCiphertext()), StandardCharsets.UTF_8))).toList();
        return new PreparedRequest(conversation, learner, reservation.eventId(), null,
                new AiBuddyClient.AiBuddyRequest(UUID.fromString(CorrelationId.current()), requestId, conversation.getScenario(), content.trim(), context));
    }
    private MessagePairView succeed(PreparedRequest prepared, AiBuddyClient.AiBuddyResponse response) {
        if (!prepared.privateRequest().correlationId().equals(response.correlationId())) throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY, "PROVIDER_ERROR", "AI response could not be validated.");
        AiConversationEntity conversation = conversations.ownedActive(prepared.learner().getUserId(), prepared.conversation().getId()); Instant now = Instant.now();
        AiMessageEntity learner = messages.findById(prepared.learner().getId()).orElseThrow(ApiException::notFound); learner.complete(now);
        AiMessageEntity assistant = AiMessageEntity.assistant(conversation.getId(), conversation.getUserId(), learner.getSequenceNumber() + 1, prepared.eventId(),
                crypto.encryptForConversation(conversation, response.chineseResponse().getBytes(StandardCharsets.UTF_8)),
                crypto.encryptForConversation(conversation, response.vietnameseExplanation().getBytes(StandardCharsets.UTF_8)),
                response.suggestion() == null ? null : crypto.encryptForConversation(conversation, response.suggestion().getBytes(StandardCharsets.UTF_8)), now);
        messages.save(learner); messages.save(assistant); conversation.touchCompletedMessages(now); allowance.succeed(prepared.eventId(), response.correlationId().toString());
        return pair(conversation, learner, assistant);
    }
    private void fail(PreparedRequest prepared, String code) {
        AiMessageEntity learner = messages.findById(prepared.learner().getId()).orElse(null);
        if (learner != null && learner.getStatus() == AiMessageStatus.PENDING) { learner.fail(code, Instant.now()); messages.save(learner); }
        allowance.refundOnce(prepared.eventId(), code);
    }
    private MessagePairView pair(AiConversationEntity conversation, AiMessageEntity learner, AiMessageEntity assistant) {
        return new MessagePairView(message(conversation, learner), message(conversation, assistant));
    }
    private AiConversationService.MessageView message(AiConversationEntity conversation, AiMessageEntity message) { return new AiConversationService.MessageView(message.getId(), message.getSequenceNumber(), message.getSender(), message.getStatus(), new String(crypto.decryptForConversation(conversation, message.getContentCiphertext()), StandardCharsets.UTF_8), message.getVietnameseExplanationCiphertext() == null ? null : new String(crypto.decryptForConversation(conversation, message.getVietnameseExplanationCiphertext()), StandardCharsets.UTF_8), message.getSuggestionCiphertext() == null ? null : new String(crypto.decryptForConversation(conversation, message.getSuggestionCiphertext()), StandardCharsets.UTF_8), message.getCreatedAt(), message.getCompletedAt()); }
    private String fingerprint(UUID conversationId, String content) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((conversationId + ":" + content.trim()).getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
    private record PreparedRequest(AiConversationEntity conversation, AiMessageEntity learner, UUID eventId, MessagePairView replay, AiBuddyClient.AiBuddyRequest privateRequest) {
        private PreparedRequest(AiConversationEntity conversation, AiMessageEntity learner, UUID eventId, MessagePairView replay) { this(conversation, learner, eventId, replay, null); }
    }
    public record MessagePairView(AiConversationService.MessageView learnerMessage, AiConversationService.MessageView assistantMessage) { }
}
