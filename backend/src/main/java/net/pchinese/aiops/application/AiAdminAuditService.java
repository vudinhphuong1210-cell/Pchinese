package net.pchinese.aiops.application;

import net.pchinese.aiops.domain.AiAdminAuditEventType;
import net.pchinese.aiops.domain.AiAdminAuditTargetType;
import net.pchinese.aiops.persistence.AiAdminAuditEventEntity;
import net.pchinese.aiops.persistence.AiAdminAuditEventRepository;
import net.pchinese.common.api.CorrelationId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class AiAdminAuditService {
    private final AiAdminAuditEventRepository audits;

    public AiAdminAuditService(AiAdminAuditEventRepository audits) {
        this.audits = audits;
    }

    public void record(UUID actorId, AiAdminAuditEventType type, AiAdminAuditTargetType targetType,
                       UUID targetId, String reasonOrNote, Map<String, Object> before,
                       Map<String, Object> after, Instant now) {
        audits.save(AiAdminAuditEventEntity.success(actorId, type, targetType, targetId,
                correlationId(), reasonOrNote, before, after, now));
    }

    private UUID correlationId() {
        try {
            return UUID.fromString(CorrelationId.current());
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
