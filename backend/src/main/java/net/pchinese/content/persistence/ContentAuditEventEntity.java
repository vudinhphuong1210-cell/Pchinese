package net.pchinese.content.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** Immutable record of an F04 administration decision. */
@Entity
@Immutable
@Table(name = "content_audit_events")
public class ContentAuditEventEntity {

    @Id
    @Column(name = "content_audit_event_id", nullable = false, updatable = false)
    private UUID contentAuditEventId;
    @Column(name = "event_type", nullable = false, updatable = false, length = 100)
    private String eventType;
    @Column(name = "actor_user_id", nullable = false, updatable = false)
    private UUID actorUserId;
    @Column(name = "target_entity_type", nullable = false, updatable = false, length = 50)
    private String targetEntityType;
    @Column(name = "target_entity_id", nullable = false, updatable = false)
    private UUID targetEntityId;
    @Column(name = "outcome", nullable = false, updatable = false, length = 20)
    private String outcome;
    @Column(name = "reason_code", updatable = false, length = 100)
    private String reasonCode;
    @Column(name = "expected_version", updatable = false)
    private Long expectedVersion;
    @Column(name = "observed_version", updatable = false)
    private Long observedVersion;
    @Column(name = "before_state", updatable = false, length = 50)
    private String beforeState;
    @Column(name = "after_state", updatable = false, length = 50)
    private String afterState;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "safe_details", columnDefinition = "jsonb", updatable = false)
    private JsonNode safeDetails;
    @Column(name = "correlation_id", nullable = false, updatable = false)
    private UUID correlationId;
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected ContentAuditEventEntity() { }

    public static ContentAuditEventEntity create(String eventType, UUID actorUserId, String targetEntityType,
                                                  UUID targetEntityId, String outcome, String reasonCode,
                                                  Long expectedVersion, Long observedVersion,
                                                  String beforeState, String afterState, JsonNode safeDetails,
                                                  UUID correlationId, Instant occurredAt) {
        ContentAuditEventEntity event = new ContentAuditEventEntity();
        event.contentAuditEventId = UUID.randomUUID();
        event.eventType = eventType;
        event.actorUserId = actorUserId;
        event.targetEntityType = targetEntityType;
        event.targetEntityId = targetEntityId;
        event.outcome = outcome;
        event.reasonCode = reasonCode;
        event.expectedVersion = expectedVersion;
        event.observedVersion = observedVersion;
        event.beforeState = beforeState;
        event.afterState = afterState;
        event.safeDetails = safeDetails;
        event.correlationId = correlationId;
        event.occurredAt = occurredAt;
        return event;
    }

    public UUID getContentAuditEventId() { return contentAuditEventId; }
    public String getEventType() { return eventType; }
    public UUID getActorUserId() { return actorUserId; }
    public String getTargetEntityType() { return targetEntityType; }
    public UUID getTargetEntityId() { return targetEntityId; }
    public String getOutcome() { return outcome; }
    public String getReasonCode() { return reasonCode; }
    public Long getExpectedVersion() { return expectedVersion; }
    public Long getObservedVersion() { return observedVersion; }
    public String getBeforeState() { return beforeState; }
    public String getAfterState() { return afterState; }
    public JsonNode getSafeDetails() { return safeDetails; }
    public UUID getCorrelationId() { return correlationId; }
    public Instant getOccurredAt() { return occurredAt; }
}
