package net.pchinese.content.api;

import net.pchinese.common.api.ApiEnvelope;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = {"net.pchinese.content", "net.pchinese.media"})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContentAdminExceptionHandler {

    @ExceptionHandler(StateConflictException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleStateConflict(StateConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiEnvelope.failure("STATE_CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiEnvelope.failure("STATE_CONFLICT", "Stale expectedVersion; resource was updated concurrently."));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiEnvelope.failure("RESOURCE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ContentValidationException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleValidation(ContentValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiEnvelope.failure("VALIDATION_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleMethodNotValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiEnvelope.failure("VALIDATION_ERROR", msg));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiEnvelope<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiEnvelope.failure("AUTHORIZATION_DENIED", "ADMIN role required for content management."));
    }
}
