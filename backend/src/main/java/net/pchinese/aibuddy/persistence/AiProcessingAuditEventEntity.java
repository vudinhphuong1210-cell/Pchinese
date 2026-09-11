package net.pchinese.aibuddy.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_processing_audit_events")
public class AiProcessingAuditEventEntity {
    @Id @Column(name = "ai_processing_audit_event_id", nullable = false, updatable = false) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "ai_conversation_id") private UUID conversationId;
    @Column(name = "correlation_id", nullable = false) private UUID correlationId;
    @Column(name = "provider_code", nullable = false, length = 64) private String providerCode;
    @Column(name = "model_code", nullable = false, length = 128) private String modelCode;
    @Column(name = "transfer_region", nullable = false, length = 64) private String transferRegion;
    @Column(name = "event_type", nullable = false, length = 40) private String eventType;
    @Column(nullable = false, length = 20) private String outcome;
    @Column(name = "safe_reason_code", length = 100) private String safeReasonCode;
    @Column(name = "provider_request_reference_hash", length = 64) private String providerRequestReferenceHash;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    protected AiProcessingAuditEventEntity() { }
    public static AiProcessingAuditEventEntity event(UUID userId, UUID conversationId, UUID correlationId, String eventType, String outcome, String safeReasonCode, Instant now) {
        AiProcessingAuditEventEntity value = new AiProcessingAuditEventEntity(); value.id = UUID.randomUUID(); value.userId = userId;
        value.conversationId = conversationId; value.correlationId = correlationId; value.providerCode = "DEEPSEEK";
        value.modelCode = "DEPLOYMENT_CONFIGURED"; value.transferRegion = "DEPLOYMENT_CONFIGURED"; value.eventType = eventType;
        value.outcome = outcome; value.safeReasonCode = safeReasonCode; value.occurredAt = now; return value;
    }
}
