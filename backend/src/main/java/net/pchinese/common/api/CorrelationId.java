package net.pchinese.common.api;

import org.slf4j.MDC;

import java.util.UUID;

public final class CorrelationId {
    public static final String HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";

    private CorrelationId() { }

    public static String current() {
        String value = MDC.get(MDC_KEY);
        return value == null ? UUID.randomUUID().toString() : value;
    }

    public static void set(String value) { MDC.put(MDC_KEY, value); }
    public static void clear() { MDC.remove(MDC_KEY); }
}
