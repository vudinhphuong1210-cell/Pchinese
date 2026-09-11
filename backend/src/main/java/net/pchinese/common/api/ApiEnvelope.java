package net.pchinese.common.api;

import java.util.Map;

public record ApiEnvelope<T>(boolean success, T data, ApiError error, Map<String, Object> meta) {
    public static <T> ApiEnvelope<T> success(T data) {
        return new ApiEnvelope<>(true, data, null, Map.<String, Object>of("correlationId", CorrelationId.current()));
    }

    public static <T> ApiEnvelope<T> success(T data, Map<String, Object> meta) {
        java.util.LinkedHashMap<String, Object> resolvedMeta = new java.util.LinkedHashMap<>();
        resolvedMeta.put("correlationId", CorrelationId.current());
        if (meta != null) {
            resolvedMeta.putAll(meta);
        }
        return new ApiEnvelope<>(true, data, null, Map.copyOf(resolvedMeta));
    }

    public static <T> ApiEnvelope<T> failure(String code, String message) {
        return new ApiEnvelope<>(false, null, new ApiError(code, message),
                Map.<String, Object>of("correlationId", CorrelationId.current()));
    }

    public record ApiError(String code, String message) { }
}
