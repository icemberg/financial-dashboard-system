package com.financedashboard.zorvyn.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.financedashboard.zorvyn.dto.ErrorResponse;
import com.financedashboard.zorvyn.enums.ErrorCodeEnum;

import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for the application.
 * Centralizes exception handling across all REST controllers.
 * Converts exceptions to structured ErrorResponse format.
 *
 * Handles:
 * - UserException: User-related business logic errors
 * - FinancialDashboardException: Domain-specific exceptions
 * - MethodArgumentNotValidException: Validation errors (@Valid failures)
 * - Generic Exception: Unexpected errors
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles UserException.
     * UserException is thrown for user-related errors like:
     * - USER_NOT_FOUND (404)
     * - USER_ALREADY_EXISTS (409)
     * - UNAUTHORIZED_ACCESS (403)
     * - INVALID_USER_INPUT (400)
     * - USER_INACTIVE (403)
     *
     * Each UserException carries its own error code and HTTP status.
     *
     * @param ex the UserException to handle
     * @return ResponseEntity with ErrorResponse and appropriate HTTP status
     */
    @ExceptionHandler(UserException.class)
    public ResponseEntity<ErrorResponse> handleUserException(UserException ex) {
        log.warn("User exception: {} - {}", ex.getErrorCode(), ex.getMessage());

        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.INTERNAL_SERVER_ERROR;

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(ex.getMessage())
                .code(ex.getErrorCode())
                .build();

        return ResponseEntity.status(status).body(body);
    }

    /**
     * Handles FinancialDashboardException (parent exception class).
     * Used for domain-specific business logic errors from other modules.
     *
     * @param ex the FinancialDashboardException to handle
     * @return ResponseEntity with ErrorResponse and appropriate HTTP status
     */
    @ExceptionHandler(FinancialDashboardException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(FinancialDashboardException ex) {
        log.warn("Domain exception: {} - {}", ex.getErrorCode(), ex.getMessage());

        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.INTERNAL_SERVER_ERROR;

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(ex.getMessage())
                .code(ex.getErrorCode())
                .build();

        return ResponseEntity.status(status).body(body);
    }

    /**
     * Handles validation errors from @Valid annotation.
     * Thrown when request body validation fails.
     *
     * Common validation errors:
     * - @NotBlank: Required field is missing or empty
     * - @Email: Invalid email format
     * - @Size: String length out of bounds
     * - @NotNull: Nullable field is null
     *
     * Logs all validation errors for debugging.
     *
     * @param ex the MethodArgumentNotValidException
     * @return ResponseEntity with VALIDATION_ERROR and 400 BAD_REQUEST status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getBindingResult());

        // Build validation error message from field errors
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("Validation failed");

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .code(ErrorCodeEnum.VALIDATION_ERROR.getErrorCode())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handles all uncaught exceptions.
     * Fallback handler for unexpected errors.
     * Logs full stack trace for debugging.
     *
     * @param ex the unexpected exception
     * @return ResponseEntity with INTERNAL_ERROR and 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneric(Exception ex) {
        log.error("Unhandled exception: ", ex);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred")
                .code(ErrorCodeEnum.INTERNAL_ERROR.getErrorCode())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}