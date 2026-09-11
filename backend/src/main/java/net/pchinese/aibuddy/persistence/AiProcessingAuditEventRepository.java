package net.pchinese.aibuddy.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AiProcessingAuditEventRepository extends JpaRepository<AiProcessingAuditEventEntity, UUID> { }
