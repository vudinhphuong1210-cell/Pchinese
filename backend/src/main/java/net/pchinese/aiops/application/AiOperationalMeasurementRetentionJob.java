package net.pchinese.aiops.application;

import net.pchinese.aiops.persistence.AiOperationalMeasurementRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class AiOperationalMeasurementRetentionJob {
    private final AiOperationalMeasurementRepository measurements;

    public AiOperationalMeasurementRetentionJob(AiOperationalMeasurementRepository measurements) {
        this.measurements = measurements;
    }

    @Scheduled(cron = "0 20 3 * * *")
    @Transactional
    public void purgeExpiredMeasurements() {
        measurements.deleteByOccurredAtBefore(Instant.now().minus(4, ChronoUnit.MONTHS));
    }
}
