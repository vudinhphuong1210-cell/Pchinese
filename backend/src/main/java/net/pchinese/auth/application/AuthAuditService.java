package net.pchinese.auth.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.pchinese.auth.persistence.AuthAuditEventEntity;
import net.pchinese.auth.persistence.AuthAuditEventRepository;
import net.pchinese.common.api.CorrelationId;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthAuditService {
    private static final Set<String> AUDITED_ROLE_CODES = Set.of("ADMIN");
    private final AuthAuditEventRepository repository;
    private final ObjectMapper objectMapper;
    public AuthAuditService(AuthAuditEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository; this.objectMapper = objectMapper;
    }
    public void record(AuditEventTaxonomy.EventType eventType, UUID actor, UUID target, UUID session, JsonNode beforeRoles, JsonNode afterRoles,
                       ObjectNode details, Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("An audit occurrence time is required.");
        }
        repository.save(AuthAuditEventEntity.create(eventType.name(), actor, target, session,
                normalizedRoles(beforeRoles), normalizedRoles(afterRoles), UUID.fromString(CorrelationId.current()),
                AuditEventTaxonomy.normalizeDetails(objectMapper, eventType, details), now));
    }

    public ObjectNode details(AuditEventTaxonomy.OutcomeCode outcomeCode) {
        return AuditEventTaxonomy.details(objectMapper, outcomeCode);
    }

    public ObjectNode details(AuditEventTaxonomy.OutcomeCode outcomeCode, AuditEventTaxonomy.ReasonCode reasonCode) {
        return AuditEventTaxonomy.details(objectMapper, outcomeCode, reasonCode);
    }

    public JsonNode roles(java.util.Set<String> roles) {
        ArrayNode result = objectMapper.createArrayNode();
        if (roles == null) {
            return result;
        }
        roles.stream().sorted().forEach(role -> {
            if (!AUDITED_ROLE_CODES.contains(role)) {
                throw new IllegalArgumentException("An approved audit role code is required.");
            }
            result.add(role);
        });
        return result;
    }

    public void recordProfilePreferencesUpdated(UUID userId, UUID sessionId, Set<String> changedFields, Instant now) {
        record(AuditEventTaxonomy.EventType.PROFILE_PREFERENCES_UPDATED, userId, userId, sessionId, null, null,
                AuditEventTaxonomy.profileDetails(objectMapper, changedFields), now);
    }

    @Transactional(readOnly = true)
    public SystemActivityPage systemActivity(int page, int size) {
        PageRequest request = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("occurredAt"), Sort.Order.desc("authAuditEventId")));
        Page<AuthAuditEventEntity> result = repository.findAll(request);
        List<SystemActivityItem> items = result.getContent().stream()
                .map(event -> new SystemActivityItem(publicEventType(event.getEventType()), event.getActorUserId(),
                        event.getTargetUserId(), event.getOccurredAt()))
                .toList();
        return new SystemActivityPage(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    private String publicEventType(String eventType) {
        return switch (eventType) {
            case "LOGIN", "EMAIL_VERIFIED", "PASSWORD_RESET", "PROFILE_PREFERENCES_UPDATED",
                    "ROLE_GRANTED", "ROLE_REVOKED", "ACCOUNT_LOCKED", "ACCOUNT_UNLOCKED" -> eventType;
            default -> "SECURITY_EVENT";
        };
    }

    private JsonNode normalizedRoles(JsonNode source) {
        if (source == null) {
            return null;
        }
        if (!source.isArray()) {
            throw new IllegalArgumentException("Audit roles must be an approved array.");
        }
        Set<String> roles = new java.util.TreeSet<>();
        for (JsonNode role : source) {
            if (!role.isTextual() || !AUDITED_ROLE_CODES.contains(role.textValue())) {
                throw new IllegalArgumentException("An approved audit role code is required.");
            }
            roles.add(role.textValue());
        }
        return this.roles(roles);
    }

    public record SystemActivityItem(String eventType, UUID actorUserId, UUID targetUserId, Instant occurredAt) { }
    public record SystemActivityPage(List<SystemActivityItem> items, int page, int size, long totalItems, int totalPages) { }
}
