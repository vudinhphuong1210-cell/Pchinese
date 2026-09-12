package net.pchinese.aiops.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiAdminAuditEventRepository extends JpaRepository<AiAdminAuditEventEntity, UUID> {
    Page<AiAdminAuditEventEntity> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
