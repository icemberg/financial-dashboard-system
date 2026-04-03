package com.financedashboard.zorvyn.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.financedashboard.zorvyn.dto.ErrorResponse;
import com.financedashboard.zorvyn.enums.ErrorCodeEnum;

import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for the application.
 * Centralizes exception handling across all REST controllers.
 * Converts all exceptions to a structured ErrorResponse format.
 *
 * Handler order (Spring selects the most specific match):
 *   1. FinancialDashboardException  → domain errors (UserException is a subclass, so also caught here)
 *   2. MethodArgumentNotValidException → @Valid failures (400)
 *   3. HttpMessageNotReadableException → malformed JSON body (400)
 *   4. MethodArgumentTypeMismatchException → bad path/query params (400)
 *   5. MissingServletRequestParameterException → required param missing (400)
 *   6. AccessDeniedException → @PreAuthorize failures (403)
 *   7. HttpRequestMethodNotSupportedException → wrong HTTP method (405)
 *   8. Exception (fallback) → unexpected errors (500)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Domain Exceptions ─────────────────────────────────────────────────────

    /**
     * Handles all domain exceptions including UserException (which extends FinancialDashboardException).
     * Each exception carries its own error code and HTTP status.
     *
     * @param ex the FinancialDashboardException (or any subclass like UserException)
     * @return ResponseEntity with ErrorResponse and the exception's HTTP status
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

    // ── Validation Exceptions ─────────────────────────────────────────────────

    /**
     * Handles @Valid annotation failures (e.g., @NotBlank, @Email, @Size, @NotNull).
     * Concatenates all field-level error messages into a single response.
     *
     * @param ex the MethodArgumentNotValidException
     * @return ResponseEntity with 400 BAD_REQUEST and detailed field errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getBindingResult());

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
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
     * Handles malformed or unreadable JSON request bodies.
     * Common causes: missing body, invalid JSON syntax, wrong data types.
     *
     * @param ex the HttpMessageNotReadableException
     * @return ResponseEntity with 400 BAD_REQUEST
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Malformed or missing request body. Please check the JSON format and data types.")
                .code(ErrorCodeEnum.BAD_REQUEST.getErrorCode())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handles type mismatch in path variables or query parameters.
     * Example: passing "abc" for a Long ID, or "INVALID" for a RecordTypeEnum.
     *
     * @param ex the MethodArgumentTypeMismatchException
     * @return ResponseEntity with 400 BAD_REQUEST
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch for parameter '{}': value '{}' is not valid for type {}",
                ex.getName(), ex.getValue(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .code(ErrorCodeEnum.BAD_REQUEST.getErrorCode())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handles missing required query parameters.
     *
     * @param ex the MissingServletRequestParameterException
     * @return ResponseEntity with 400 BAD_REQUEST
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Missing required parameter: {}", ex.getParameterName());

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Required parameter '" + ex.getParameterName() + "' is missing.")
                .code(ErrorCodeEnum.BAD_REQUEST.getErrorCode())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ── Security Exceptions ───────────────────────────────────────────────────

    /**
     * Handles @PreAuthorize failures (role-based access denied).
     * Thrown when an authenticated user lacks the required role.
     *
     * @param ex the AccessDeniedException
     * @return ResponseEntity with 403 FORBIDDEN
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message(ErrorCodeEnum.UNAUTHORIZED_ACCESS.getErrorMessage())
                .code(ErrorCodeEnum.UNAUTHORIZED_ACCESS.getErrorCode())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // ── HTTP Method Errors ────────────────────────────────────────────────────

    /**
     * Handles wrong HTTP method (e.g., GET on a POST-only endpoint).
     *
     * @param ex the HttpRequestMethodNotSupportedException
     * @return ResponseEntity with 405 METHOD_NOT_ALLOWED
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not allowed: {} for this endpoint", ex.getMethod());

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error(HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase())
                .message("HTTP method '" + ex.getMethod() + "' is not supported for this endpoint.")
                .code(ErrorCodeEnum.BAD_REQUEST.getErrorCode())
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    // ── Fallback ──────────────────────────────────────────────────────────────

    /**
     * Handles all uncaught exceptions.
     * Returns a generic 500 error without leaking internal details.
     * Full stack trace is logged server-side for debugging.
     *
     * @param ex the unexpected exception
     * @return ResponseEntity with 500 INTERNAL_SERVER_ERROR
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception: ", ex);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred. Please try again later.")
                .code(ErrorCodeEnum.INTERNAL_ERROR.getErrorCode())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}