package net.pchinese.common.error;

import jakarta.validation.ConstraintViolationException;
import net.pchinese.common.api.ApiEnvelope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiEnvelope<Void>> handleApi(ApiException exception) {
        return ResponseEntity.status(exception.status()).body(ApiEnvelope.failure(exception.code(), exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    ResponseEntity<ApiEnvelope<Void>> handleValidation(Exception exception) {
        return ResponseEntity.badRequest().body(ApiEnvelope.failure("VALIDATION_ERROR", "Request validation failed."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiEnvelope<Void>> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(ApiEnvelope.failure("VALIDATION_ERROR", "Request validation failed."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiEnvelope<Void>> handleDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiEnvelope.failure(
                "AUTHORIZATION_DENIED", "You are not authorized for this action."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiEnvelope<Void>> handleUnexpected(Exception exception) {
        log.error("Unhandled API exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiEnvelope.failure(
                "INTERNAL_ERROR", "The request could not be completed."));
    }
}
