package net.pchinese.aiops.application;

import net.pchinese.aiops.domain.AiAdminAuditEventType;
import net.pchinese.aiops.domain.AiAdminAuditTargetType;
import net.pchinese.aiops.persistence.AiAdminAuditEventRepository;
import net.pchinese.common.error.ApiException;
import net.pchinese.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AiAdminAuditQueryService {
    private final AiAdminAuditEventRepository audits;

    public AiAdminAuditQueryService(AiAdminAuditEventRepository audits) { this.audits = audits; }

    @Transactional(readOnly = true)
    public Page<AuditEventView> list(UserPrincipal actor, Pageable pageable) {
        if (actor == null) throw ApiException.unauthenticated();
        if (!actor.isAdmin()) throw ApiException.forbidden();
        return audits.findAllByOrderByOccurredAtDesc(pageable).map(event -> new AuditEventView(event.getActorUserId(),
                event.getEventType(), event.getTargetType(), event.getTargetId(), event.getOccurredAt(),
                event.getReasonOrNote(), "SUCCESS"));
    }

    public record AuditEventView(UUID actorUserId, AiAdminAuditEventType eventType,
                                 AiAdminAuditTargetType targetType, UUID targetId,
                                 Instant occurredAt, String reasonOrNote, String outcome) { }
}
