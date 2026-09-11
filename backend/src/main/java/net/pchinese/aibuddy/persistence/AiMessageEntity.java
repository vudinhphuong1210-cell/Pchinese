package net.pchinese.aibuddy.persistence;

import jakarta.persistence.*;
import net.pchinese.aibuddy.domain.AiMessageSender;
import net.pchinese.aibuddy.domain.AiMessageStatus;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_messages")
public class AiMessageEntity {
    @Id @Column(name = "ai_message_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "ai_conversation_id", nullable = false, updatable = false) private UUID conversationId;
    @Column(name = "user_id", nullable = false, updatable = false) private UUID userId;
    @Column(name = "sequence_no", nullable = false) private int sequenceNumber;
    @Enumerated(EnumType.STRING) @Column(name = "sender_type", nullable = false, length = 16) private AiMessageSender sender;
    @Column(name = "content_ciphertext", nullable = false) private byte[] contentCiphertext;
    @Column(name = "vietnamese_explanation_ciphertext") private byte[] vietnameseExplanationCiphertext;
    @Column(name = "suggestion_ciphertext") private byte[] suggestionCiphertext;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private AiMessageStatus status;
    @Column(name = "client_request_id") private UUID clientRequestId;
    @JdbcTypeCode(Types.CHAR) @Column(name = "request_fingerprint", length = 64) private String requestFingerprint;
    @Column(name = "failure_code", length = 100) private String failureCode;
    @Column(name = "ai_usage_event_id") private UUID aiUsageEventId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected AiMessageEntity() { }
    public static AiMessageEntity learner(UUID conversationId, UUID userId, int sequence, UUID requestId,
                                          String fingerprint, byte[] content, Instant now) {
        AiMessageEntity value = base(conversationId, userId, sequence, AiMessageSender.LEARNER, content, now);
        value.clientRequestId = requestId; value.requestFingerprint = fingerprint; value.status = AiMessageStatus.PENDING;
        return value;
    }
    public static AiMessageEntity assistant(UUID conversationId, UUID userId, int sequence, UUID usageEventId,
                                            byte[] content, byte[] explanation, byte[] suggestion, Instant now) {
        AiMessageEntity value = base(conversationId, userId, sequence, AiMessageSender.ASSISTANT, content, now);
        value.aiUsageEventId = usageEventId; value.vietnameseExplanationCiphertext = explanation;
        value.suggestionCiphertext = suggestion; value.status = AiMessageStatus.COMPLETE; value.completedAt = now;
        return value;
    }
    private static AiMessageEntity base(UUID conversationId, UUID userId, int sequence, AiMessageSender sender, byte[] content, Instant now) {
        AiMessageEntity value = new AiMessageEntity(); value.id = UUID.randomUUID(); value.conversationId = conversationId;
        value.userId = userId; value.sequenceNumber = sequence; value.sender = sender; value.contentCiphertext = content;
        value.createdAt = now; value.updatedAt = now; return value;
    }
    public UUID getId() { return id; } public UUID getConversationId() { return conversationId; } public UUID getUserId() { return userId; }
    public int getSequenceNumber() { return sequenceNumber; } public AiMessageSender getSender() { return sender; }
    public byte[] getContentCiphertext() { return contentCiphertext; } public byte[] getVietnameseExplanationCiphertext() { return vietnameseExplanationCiphertext; }
    public byte[] getSuggestionCiphertext() { return suggestionCiphertext; } public AiMessageStatus getStatus() { return status; }
    public UUID getClientRequestId() { return clientRequestId; } public String getRequestFingerprint() { return requestFingerprint; }
    public Instant getCreatedAt() { return createdAt; } public Instant getCompletedAt() { return completedAt; }
    public void complete(Instant now) { status = AiMessageStatus.COMPLETE; completedAt = now; updatedAt = now; }
    public void fail(String code, Instant now) { status = AiMessageStatus.FAILED; failureCode = code; updatedAt = now; }
}
