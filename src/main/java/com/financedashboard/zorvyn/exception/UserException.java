package com.financedashboard.zorvyn.exception;

import org.springframework.http.HttpStatus;

import com.financedashboard.zorvyn.enums.ErrorCodeEnum;

/**
 * Custom exception for user-related operations.
 * Extends FinancialDashboardException to provide structured error handling.
 * Used for scenarios like: user not found, duplicate email, unauthorized access, etc.
 */
public class UserException extends FinancialDashboardException {

    /**
     * Constructor with message, error code, and HTTP status.
     *
     * @param message the error message
     * @param code the error code from ErrorCodeEnum
     * @param status the HTTP status to return
     */
    public UserException(String message, ErrorCodeEnum code, HttpStatus status) {
        super(message, code, status);
    }

    /**
     * Constructor with message and error code (status derived from ErrorCodeEnum).
     *
     * @param message the error message
     * @param code the error code from ErrorCodeEnum
     */
    public UserException(String message, ErrorCodeEnum code) {
        super(message, code, code.getHttpStatus());
    }

    /**
     * Constructor with error code (numeric string), message, and HTTP status.
     * This constructor is used when throwing exceptions with structured error format.
     * Example: new UserException(
     *              ErrorCodeEnum.USER_ALREADY_EXISTS.getCode(),
     *              ErrorCodeEnum.USER_ALREADY_EXISTS.getErrorMessage() + ": " + email,
     *              ErrorCodeEnum.USER_ALREADY_EXISTS.getHttpStatus()
     *          )
     *
     * @param errorCode the numeric error code (e.g., "20001")
     * @param errorMessage the detailed error message
     * @param status the HTTP status to return
     */
    public UserException(String errorCode, String errorMessage, HttpStatus status) {
        super(errorCode, errorMessage, status, ErrorCodeEnum.INVALID_USER_INPUT);
    }
}
