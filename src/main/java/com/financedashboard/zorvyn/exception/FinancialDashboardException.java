package com.financedashboard.zorvyn.exception;

import org.springframework.http.HttpStatus;

import com.financedashboard.zorvyn.enums.ErrorCodeEnum;

/**
 * Base exception class for Finance Dashboard application.
 * All domain-specific exceptions should extend this class.
 */
public class FinancialDashboardException extends RuntimeException {

    private static final long serialVersionUID = 1L;
	private final ErrorCodeEnum code;
    private final HttpStatus status;
    private String errorCode; // Numeric error code (e.g., "20001")

    /**
     * Constructor with message, error code enum, and HTTP status.
     *
     * @param message the error message
     * @param code the error code from ErrorCodeEnum
     * @param status the HTTP status to return
     */
    public FinancialDashboardException(String message, ErrorCodeEnum code, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
        this.errorCode = code.getErrorCode();
    }

    /**
     * Constructor with error code (numeric string), message, and HTTP status.
     * Used for structured error format with numeric codes.
     *
     * @param errorCode the numeric error code (e.g., "20001")
     * @param message the detailed error message
     * @param status the HTTP status to return
     */
    public FinancialDashboardException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.code = ErrorCodeEnum.INTERNAL_ERROR; // Default fallback
        this.status = status;
    }

    /**
     * Constructor with error code (numeric string), message, and HTTP status.
     * Used for structured error format with numeric codes and error code enum.
     *
     * @param errorCode the numeric error code (e.g., "20001")
     * @param message the detailed error message
     * @param status the HTTP status to return
     * @param code the ErrorCodeEnum for backward compatibility
     */
    public FinancialDashboardException(String errorCode, String message, HttpStatus status, ErrorCodeEnum code) {
        super(message);
        this.errorCode = errorCode;
        this.code = code;
        this.status = status;
    }

    public ErrorCodeEnum getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}