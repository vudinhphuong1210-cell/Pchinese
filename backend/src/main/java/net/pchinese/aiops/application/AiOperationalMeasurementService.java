package net.pchinese.aiops.application;

import net.pchinese.aiops.domain.AiCapability;
import net.pchinese.aiops.domain.OperationalMeasurementOutcome;
import net.pchinese.aiops.domain.ProviderTelemetry;
import net.pchinese.aiops.domain.ProviderTelemetryValidator;
import net.pchinese.aiops.persistence.AiOperationalMeasurementEntity;
import net.pchinese.aiops.persistence.AiOperationalMeasurementRepository;
import net.pchinese.aiops.persistence.PlanPolicyVersionEntity;
import net.pchinese.aiops.persistence.PlanPolicyVersionRepository;
import net.pchinese.allowance.persistence.AiUsageEventEntity;
import net.pchinese.security.PchineseSecurityProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AiOperationalMeasurementService {
    private final AiOperationalMeasurementRepository measurements;
    private final ProviderTelemetryValidator telemetryValidator;
    private final PchineseSecurityProperties securityProperties;
    private final PlanPolicyVersionRepository policies;

    public AiOperationalMeasurementService(AiOperationalMeasurementRepository measurements,
                                           ProviderTelemetryValidator telemetryValidator,
                                           PchineseSecurityProperties securityProperties,
                                           PlanPolicyVersionRepository policies) {
        this.measurements = measurements;
        this.telemetryValidator = telemetryValidator;
        this.securityProperties = securityProperties;
        this.policies = policies;
    }

    @Transactional
    public void createOrReusePending(AiUsageEventEntity event, Instant now) {
        String key = measurementKey("usage:" + event.getAiUsageEventId());
        if (measurements.findByMeasurementKey(key).isEmpty()) {
            measurements.save(AiOperationalMeasurementEntity.pending(key, event, now));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createOrReuseQuotaDenied(UUID userId, UUID clientRequestId, UUID policyVersionId,
                                          AiCapability capability, Instant now) {
        String key = measurementKey("denied:" + userId + ":" + clientRequestId + ":" + capability.name());
        if (measurements.findByMeasurementKey(key).isEmpty()) {
            measurements.save(AiOperationalMeasurementEntity.quotaDenied(key, policies.getReferenceById(policyVersionId), capability, now));
        }
    }

    @Transactional
    public void finalizeSuccess(AiUsageEventEntity event, ProviderTelemetry telemetry, long endToEndDurationMs, Instant now) {
        finalizeEvent(event, OperationalMeasurementOutcome.SUCCEEDED, telemetry, endToEndDurationMs, now);
    }

    @Transactional
    public void finalizeRefund(AiUsageEventEntity event, ProviderTelemetry telemetry, long endToEndDurationMs, Instant now) {
        finalizeEvent(event, OperationalMeasurementOutcome.FAILED_REFUNDED, telemetry, endToEndDurationMs, now);
    }

    private void finalizeEvent(AiUsageEventEntity event, OperationalMeasurementOutcome outcome,
                               ProviderTelemetry telemetry, long endToEndDurationMs, Instant now) {
        String key = measurementKey("usage:" + event.getAiUsageEventId());
        AiOperationalMeasurementEntity measurement = measurements.findByMeasurementKey(key)
                .orElseGet(() -> measurements.save(AiOperationalMeasurementEntity.pending(key, event, now)));
        measurement.finalizeOutcome(outcome, telemetryValidator.sanitize(telemetry), endToEndDurationMs, now);
        measurements.save(measurement);
    }

    private String measurementKey(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            String secret = securityProperties.getTokenPepper();
            if (secret == null || secret.isBlank()) throw new IllegalStateException("Measurement HMAC secret is unavailable.");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Operational measurement key could not be derived.", exception);
        }
    }
}
