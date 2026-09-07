package net.pchinese.auth.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.pchinese.auth.persistence.AuthAuditEventEntity;
import net.pchinese.auth.persistence.AuthAuditEventRepository;
import net.pchinese.common.api.CorrelationId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthAuditService {
    private final AuthAuditEventRepository repository;
    private final ObjectMapper objectMapper;
    public AuthAuditService(AuthAuditEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository; this.objectMapper = objectMapper;
    }
    public void record(String eventType, UUID actor, UUID target, UUID session, JsonNode beforeRoles, JsonNode afterRoles,
                       ObjectNode details, Instant now) {
        repository.save(AuthAuditEventEntity.create(eventType, actor, target, session, beforeRoles, afterRoles,
                UUID.fromString(CorrelationId.current()), details, now));
    }
    public ObjectNode details(String outcome) {
        ObjectNode details = objectMapper.createObjectNode(); details.put("outcome", outcome); return details;
    }
    public JsonNode roles(java.util.Set<String> roles) { return objectMapper.valueToTree(roles.stream().sorted().toList()); }
}
