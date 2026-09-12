package net.pchinese.aiops.domain;

import java.math.BigDecimal;

/**
 * Minimized private provider metering. It deliberately has no provider payload, learner content,
 * correlation id or credential field.
 */
public record ProviderTelemetry(
        Long inputTokens,
        Long outputTokens,
        Long totalTokens,
        BigDecimal estimatedCost,
        String costCurrency,
        Long providerDurationMs,
        String failureClass
) {
    public static ProviderTelemetry unavailable() {
        return new ProviderTelemetry(null, null, null, null, null, null, null);
    }
}
