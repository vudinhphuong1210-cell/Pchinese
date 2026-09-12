package net.pchinese.aiops.domain;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

@Component
public class ProviderTelemetryValidator {
    private static final Set<String> FAILURE_CLASSES = Set.of("timeout", "unavailable", "invalid-output", "safety-rejected");

    /** Invalid optional telemetry is unavailable rather than a fabricated zero. */
    public ProviderTelemetry sanitize(ProviderTelemetry telemetry) {
        if (telemetry == null) {
            return ProviderTelemetry.unavailable();
        }
        if (negative(telemetry.inputTokens()) || negative(telemetry.outputTokens()) || negative(telemetry.totalTokens())
                || negative(telemetry.providerDurationMs()) || negative(telemetry.estimatedCost())) {
            return ProviderTelemetry.unavailable();
        }
        if ((telemetry.estimatedCost() == null) != (telemetry.costCurrency() == null)) {
            return ProviderTelemetry.unavailable();
        }
        String currency = telemetry.costCurrency() == null ? null : telemetry.costCurrency().toUpperCase(Locale.ROOT);
        if (currency != null && !currency.matches("[A-Z]{3}")) {
            return ProviderTelemetry.unavailable();
        }
        if (telemetry.totalTokens() != null && telemetry.inputTokens() != null && telemetry.outputTokens() != null
                && telemetry.totalTokens() != telemetry.inputTokens() + telemetry.outputTokens()) {
            return ProviderTelemetry.unavailable();
        }
        String failureClass = telemetry.failureClass();
        if (failureClass != null && !FAILURE_CLASSES.contains(failureClass)) {
            return ProviderTelemetry.unavailable();
        }
        return new ProviderTelemetry(telemetry.inputTokens(), telemetry.outputTokens(), telemetry.totalTokens(),
                telemetry.estimatedCost(), currency, telemetry.providerDurationMs(), failureClass);
    }

    private boolean negative(Long value) {
        return value != null && value < 0;
    }

    private boolean negative(BigDecimal value) {
        return value != null && value.signum() < 0;
    }
}
