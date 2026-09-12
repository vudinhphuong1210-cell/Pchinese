package net.pchinese.aiops.persistence;

import net.pchinese.aiops.domain.OperationalMeasurementOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiOperationalMeasurementRepository extends JpaRepository<AiOperationalMeasurementEntity, UUID> {
    Optional<AiOperationalMeasurementEntity> findByMeasurementKey(String measurementKey);
    Optional<AiOperationalMeasurementEntity> findByUsageEvent_AiUsageEventId(UUID usageEventId);
    @Query("select measurement from AiOperationalMeasurementEntity measurement where measurement.outcome <> 'PENDING' and measurement.finalizedAt >= :fromInclusive and measurement.finalizedAt < :toExclusive")
    List<AiOperationalMeasurementEntity> findFinalizedBetween(Instant fromInclusive, Instant toExclusive);
    @Query("select count(measurement) > 0 from AiOperationalMeasurementEntity measurement where measurement.outcome = 'PENDING' and measurement.occurredAt < :toExclusive")
    boolean existsPendingBefore(Instant toExclusive);
    long deleteByOccurredAtBefore(Instant threshold);
    long countByOutcomeAndOccurredAtBefore(OperationalMeasurementOutcome outcome, Instant threshold);
}
