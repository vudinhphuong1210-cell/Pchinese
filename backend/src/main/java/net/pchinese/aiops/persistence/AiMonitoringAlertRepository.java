package net.pchinese.aiops.persistence;

import net.pchinese.aiops.domain.AlertState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiMonitoringAlertRepository extends JpaRepository<AiMonitoringAlertEntity, UUID> {
    Optional<AiMonitoringAlertEntity> findByRule_IdAndStateIn(UUID ruleId, java.util.Collection<AlertState> states);
    Page<AiMonitoringAlertEntity> findAllByOrderByLastEvaluatedAtDesc(Pageable pageable);
    Page<AiMonitoringAlertEntity> findByStateOrderByLastEvaluatedAtDesc(AlertState state, Pageable pageable);
}
