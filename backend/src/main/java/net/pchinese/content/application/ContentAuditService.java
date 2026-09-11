package net.pchinese.content.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.pchinese.common.api.CorrelationId;
import net.pchinese.content.persistence.ContentAuditEventEntity;
import net.pchinese.content.persistence.ContentAuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ContentAuditService {

    private final ContentAuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public ContentAuditService(ContentAuditEventRepository auditEventRepository, ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
    }

    /** Compatibility entry point for non-lifecycle edits; records only safe, structured details. */
    @Transactional
    public void recordEvent(String eventType, UUID actorUserId, String targetEntityType,
                            UUID targetEntityId, Long version, String outcome,
                            UUID ignoredCorrelationId, String detailsJson) {
        record(eventType, actorUserId, targetEntityType, targetEntityId, outcome, null,
                null, version, null, null, safeDetails(detailsJson));
    }

    @Transactional
    public void record(String eventType, UUID actorUserId, String targetEntityType, UUID targetEntityId,
                       String outcome, String reasonCode, Long expectedVersion, Long observedVersion,
                       String beforeState, String afterState, ObjectNode safeDetails) {
        auditEventRepository.save(ContentAuditEventEntity.create(eventType, actorUserId,
                normalizeTargetType(targetEntityType), targetEntityId, outcome, reasonCode,
                expectedVersion, observedVersion, beforeState, afterState, safeDetails,
                UUID.fromString(CorrelationId.current()), Instant.now()));
    }

    /** Persists an allowed rejected decision after a business transaction has rolled back. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRejected(String eventType, UUID actorUserId, String targetEntityType, UUID targetEntityId,
                               String reasonCode, Long expectedVersion, Long observedVersion,
                               String beforeState, ObjectNode safeDetails) {
        record(eventType, actorUserId, targetEntityType, targetEntityId, "REJECTED", reasonCode,
                expectedVersion, observedVersion, beforeState, beforeState, safeDetails);
    }

    private ObjectNode safeDetails(String detailsJson) {
        if (detailsJson == null || detailsJson.isBlank()) {
            return null;
        }
        try {
            JsonNode parsed = objectMapper.readTree(detailsJson);
            if (parsed instanceof ObjectNode objectNode) {
                return objectNode;
            }
        } catch (Exception ignored) {
            // Audit metadata is optional. Unstructured input is intentionally not persisted.
        }
        return null;
    }

    private String normalizeTargetType(String targetEntityType) {
        return "MEDIA".equals(targetEntityType) ? "MEDIA_ASSET" : targetEntityType;
    }
}
