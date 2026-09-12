package net.pchinese.aiops;

import net.pchinese.aiops.domain.ProviderTelemetry;
import net.pchinese.aiops.domain.ProviderTelemetryValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProviderTelemetryContractTest {
    private final ProviderTelemetryValidator validator = new ProviderTelemetryValidator();

    @Test
    void preservesMeasuredZeroAndValidMetering() {
        ProviderTelemetry telemetry = validator.sanitize(new ProviderTelemetry(0L, 0L, 0L,
                BigDecimal.ZERO, "usd", 0L, null));

        assertEquals(0L, telemetry.totalTokens());
        assertEquals(BigDecimal.ZERO, telemetry.estimatedCost());
        assertEquals("USD", telemetry.costCurrency());
    }

    @Test
    void convertsMalformedOrInconsistentTelemetryToUnavailable() {
        ProviderTelemetry telemetry = validator.sanitize(new ProviderTelemetry(4L, 3L, 99L,
                null, null, 12L, null));

        assertNull(telemetry.inputTokens());
        assertNull(telemetry.totalTokens());
        assertNull(telemetry.estimatedCost());
    }

    @Test
    void rejectsUnsafeFailureClassAndUnpairedCost() {
        ProviderTelemetry telemetry = validator.sanitize(new ProviderTelemetry(null, null, null,
                BigDecimal.ONE, null, null, "provider-stack-trace"));

        assertNull(telemetry.failureClass());
        assertNull(telemetry.costCurrency());
    }
}
