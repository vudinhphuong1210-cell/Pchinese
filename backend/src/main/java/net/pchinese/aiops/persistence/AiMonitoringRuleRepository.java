package net.pchinese.aiops.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiMonitoringRuleRepository extends JpaRepository<AiMonitoringRuleEntity, UUID> {
    List<AiMonitoringRuleEntity> findByEnabledTrue();
    List<AiMonitoringRuleEntity> findAllByOrderByUpdatedAtDesc();
}
