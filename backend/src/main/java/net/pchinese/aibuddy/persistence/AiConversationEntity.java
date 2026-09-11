package net.pchinese.aibuddy.persistence;

import jakarta.persistence.*;
import net.pchinese.aibuddy.domain.AiBuddyScenario;
import net.pchinese.aibuddy.domain.AiConversationStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_conversations")
public class AiConversationEntity {
    @Id @Column(name = "ai_conversation_id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(name = "title_ciphertext")
    private byte[] titleCiphertext;
    @Column(name = "wrapped_conversation_dek")
    private byte[] wrappedConversationDek;
    @Enumerated(EnumType.STRING) @Column(name = "scenario_code", nullable = false, updatable = false, length = 64)
    private AiBuddyScenario scenario;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private AiConversationStatus status;
    @Column(name = "last_message_at") private Instant lastMessageAt;
    @Column(name = "retention_notice_sent_at") private Instant retentionNoticeSentAt;
    @Column(name = "conversation_key_destroyed_at") private Instant conversationKeyDestroyedAt;
    @Column(name = "deleted_at") private Instant deletedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(nullable = false) private long version;

    protected AiConversationEntity() { }

    public static AiConversationEntity create(UUID userId, AiBuddyScenario scenario, byte[] wrappedConversationDek,
                                               byte[] titleCiphertext, Instant now) {
        AiConversationEntity value = new AiConversationEntity();
        value.id = UUID.randomUUID(); value.userId = userId; value.scenario = scenario;
        value.wrappedConversationDek = wrappedConversationDek; value.titleCiphertext = titleCiphertext;
        value.status = AiConversationStatus.ACTIVE; value.createdAt = now; value.updatedAt = now;
        return value;
    }
    public UUID getId() { return id; } public UUID getUserId() { return userId; }
    public byte[] getTitleCiphertext() { return titleCiphertext; }
    public byte[] getWrappedConversationDek() { return wrappedConversationDek; }
    public AiBuddyScenario getScenario() { return scenario; } public AiConversationStatus getStatus() { return status; }
    public Instant getLastMessageAt() { return lastMessageAt; } public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; } public Instant getDeletedAt() { return deletedAt; }
    public boolean isActive() { return status == AiConversationStatus.ACTIVE && wrappedConversationDek != null; }
    public void rename(byte[] title, Instant now) { titleCiphertext = title; updatedAt = now; }
    public void touchCompletedMessages(Instant now) { lastMessageAt = now; updatedAt = now; }
    public void markRetentionNoticeSent(Instant now) { retentionNoticeSentAt = now; updatedAt = now; }
    public void markDeleted(Instant now) { status = AiConversationStatus.DELETED; deletedAt = now; conversationKeyDestroyedAt = now; wrappedConversationDek = null; titleCiphertext = null; updatedAt = now; }
}
