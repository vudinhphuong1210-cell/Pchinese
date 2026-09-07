package net.pchinese.common.error;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }

    public static ApiException validation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
    public static ApiException unauthenticated() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is required.");
    }
    public static ApiException accessTokenExpired() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "ACCESS_TOKEN_EXPIRED", "Access token has expired.");
    }
    public static ApiException refreshInvalid() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID", "Refresh token is invalid.");
    }
    public static ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, "AUTHORIZATION_DENIED", "You are not authorized for this action.");
    }
    public static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "The requested resource was not found.");
    }
    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, "STATE_CONFLICT", message);
    }
}
