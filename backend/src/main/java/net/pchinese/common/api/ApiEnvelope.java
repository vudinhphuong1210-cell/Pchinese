package net.pchinese.common.api;

import java.util.Map;

public record ApiEnvelope<T>(boolean success, T data, ApiError error, Map<String, String> meta) {
    public static <T> ApiEnvelope<T> success(T data) {
        return new ApiEnvelope<>(true, data, null, Map.of("correlationId", CorrelationId.current()));
    }

    public static <T> ApiEnvelope<T> failure(String code, String message) {
        return new ApiEnvelope<>(false, null, new ApiError(code, message),
                Map.of("correlationId", CorrelationId.current()));
    }

    public record ApiError(String code, String message) { }
}
