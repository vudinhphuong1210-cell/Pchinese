package net.pchinese.aiops.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import net.pchinese.aiops.domain.AiAdminAuditEventType;
import net.pchinese.aiops.domain.AiAdminAuditTargetType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ai_admin_audit_events")
public class AiAdminAuditEventEntity {
    @Id
    @Column(name = "ai_admin_audit_event_id", nullable = false, updatable = false)
    private UUID id;
    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private UUID actorUserId;
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 32)
    private AiAdminAuditEventType eventType;
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, updatable = false, length = 32)
    private AiAdminAuditTargetType targetType;
    @Column(name = "target_id", nullable = false, updatable = false)
    private UUID targetId;
    @Column(name = "correlation_id", updatable = false)
    private UUID correlationId;
    @Column(nullable = false, updatable = false, length = 16)
    private String outcome;
    @Column(name = "reason_or_note", length = 500, updatable = false)
    private String reasonOrNote;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "safe_before", columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> safeBefore;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "safe_after", columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> safeAfter;
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AiAdminAuditEventEntity() { }

    public static AiAdminAuditEventEntity success(UUID actorId, AiAdminAuditEventType eventType,
                                                   AiAdminAuditTargetType targetType, UUID targetId,
                                                   UUID correlationId, String reasonOrNote,
                                                   Map<String, Object> safeBefore,
                                                   Map<String, Object> safeAfter, Instant now) {
        AiAdminAuditEventEntity event = new AiAdminAuditEventEntity();
        event.id = UUID.randomUUID();
        event.actorUserId = actorId;
        event.eventType = eventType;
        event.targetType = targetType;
        event.targetId = targetId;
        event.correlationId = correlationId;
        event.outcome = "SUCCESS";
        event.reasonOrNote = reasonOrNote;
        event.safeBefore = safeBefore == null ? null : Map.copyOf(safeBefore);
        event.safeAfter = safeAfter == null ? null : Map.copyOf(safeAfter);
        event.occurredAt = now;
        return event;
    }

    public UUID getId() { return id; }
    public UUID getActorUserId() { return actorUserId; }
    public AiAdminAuditEventType getEventType() { return eventType; }
    public AiAdminAuditTargetType getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getReasonOrNote() { return reasonOrNote; }
}
