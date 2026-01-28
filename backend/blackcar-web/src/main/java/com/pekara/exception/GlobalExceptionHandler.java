package com.pekara.exception;

import com.pekara.dto.response.WebErrorResponse;
import com.pekara.exception.ActiveRideConflictException;
import com.pekara.exception.InvalidScheduleTimeException;
import com.pekara.exception.NoActiveDriversException;
import com.pekara.exception.NoDriversAvailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors from @Valid annotation
     * Returns a map of field names and error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        log.warn("Validation error occurred: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String errorMessage = error.getDefaultMessage();
            if (error instanceof FieldError fieldError) {
                errors.put(fieldError.getField(), errorMessage);
            } else {
                // Object-level validation error (not tied to a single field)
                errors.put(error.getObjectName(), errorMessage);
            }
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    /**
     * Handles invalid or missing JSON request bodies.
     * Example: empty body for a @RequestBody parameter, malformed JSON, wrong types.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<WebErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Invalid or unreadable request body", ex);

        WebErrorResponse error = new WebErrorResponse(
                "INVALID_REQUEST_BODY",
                "Request body is missing or invalid JSON."
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles all other unexpected exceptions
     * Returns a generic error message to avoid leaking sensitive information
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<WebErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        WebErrorResponse error = new WebErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handles IllegalArgumentException
     * Returns a bad request response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<WebErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());

        WebErrorResponse error = new WebErrorResponse(
                "INVALID_ARGUMENT",
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles BadCredentialsException from authentication
     * Returns unauthorized response
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<WebErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        WebErrorResponse error = new WebErrorResponse(
                "AUTHENTICATION_FAILED",
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles DuplicateResourceException
     * Returns conflict response
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<WebErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage());

        WebErrorResponse error = new WebErrorResponse(
                "DUPLICATE_RESOURCE",
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles InvalidTokenException
     * Returns bad request response
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<WebErrorResponse> handleInvalidToken(InvalidTokenException ex) {
        log.warn("Invalid token: {}", ex.getMessage());

        WebErrorResponse error = new WebErrorResponse(
                "INVALID_TOKEN",
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(InvalidScheduleTimeException.class)
    public ResponseEntity<WebErrorResponse> handleInvalidScheduleTime(InvalidScheduleTimeException ex) {
        WebErrorResponse error = new WebErrorResponse(
                "INVALID_SCHEDULE_TIME",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler({NoActiveDriversException.class, NoDriversAvailableException.class})
    public ResponseEntity<WebErrorResponse> handleNoDrivers(RuntimeException ex) {
        WebErrorResponse error = new WebErrorResponse(
                "NO_DRIVERS_AVAILABLE",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ActiveRideConflictException.class)
    public ResponseEntity<WebErrorResponse> handleActiveRideConflict(ActiveRideConflictException ex) {
        log.warn("Active ride conflict: {}", ex.getMessage());

        WebErrorResponse error = new WebErrorResponse(
                "ACTIVE_RIDE_CONFLICT",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
}
