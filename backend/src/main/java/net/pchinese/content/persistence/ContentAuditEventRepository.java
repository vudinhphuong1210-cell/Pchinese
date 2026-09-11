package net.pchinese.content.persistence;

import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Deliberately insert-only: audit records must never be updated or deleted. */
@Component
public interface ContentAuditEventRepository extends Repository<ContentAuditEventEntity, UUID> {
    <S extends ContentAuditEventEntity> S save(S event);
}
