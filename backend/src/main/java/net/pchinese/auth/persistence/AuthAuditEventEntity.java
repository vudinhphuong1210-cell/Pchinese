package net.pchinese.auth.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_audit_events")
public class AuthAuditEventEntity {
    @Id private UUID authAuditEventId;
    private String eventType;
    private UUID actorUserId;
    private UUID targetUserId;
    private UUID sessionId;
    @JdbcTypeCode(SqlTypes.JSON) private JsonNode beforeRoles;
    @JdbcTypeCode(SqlTypes.JSON) private JsonNode afterRoles;
    private UUID correlationId;
    @JdbcTypeCode(SqlTypes.JSON) private JsonNode details;
    private Instant occurredAt;
    protected AuthAuditEventEntity() { }
    public static AuthAuditEventEntity create(String eventType, UUID actor, UUID target, UUID session, JsonNode before, JsonNode after,
                                              UUID correlationId, JsonNode details, Instant now) {
        AuthAuditEventEntity event = new AuthAuditEventEntity(); event.authAuditEventId = UUID.randomUUID(); event.eventType = eventType;
        event.actorUserId = actor; event.targetUserId = target; event.sessionId = session; event.beforeRoles = before; event.afterRoles = after;
        event.correlationId = correlationId; event.details = details; event.occurredAt = now; return event;
    }
}
