package com.financedashboard.zorvyn.enums;

import org.springframework.http.HttpStatus;

/**
 * 
 * Error Code Structure:
 * - 1xxxx: Dashboard-related errors
 * - 2xxxx: User Management errors
 * - 3xxxx: Financial Record errors
 * - 4xxxx: Authentication & Authorization errors
 * 
 * Each error code includes:
 * - Unique error code (numeric identifier)
 * - Descriptive error message
 * - Appropriate HTTP status code
 */
public enum ErrorCodeEnum {

    DASHBOARD_NOT_FOUND("10001", "Dashboard configuration not found. Please check the dashboard ID and try again.",
            HttpStatus.NOT_FOUND),
    DASHBOARD_FETCH_FAILED("10002", "Failed to retrieve dashboard data. Please try again later or contact support.",
            HttpStatus.INTERNAL_SERVER_ERROR),
    DASHBOARD_EMPTY("10003", "Dashboard contains no data. Start by adding financial records.", HttpStatus.OK),
    DASHBOARD_CALCULATION_ERROR("10004",
            "Error calculating dashboard metrics. Invalid data detected. Please verify your financial records.",
            HttpStatus.INTERNAL_SERVER_ERROR),
    DASHBOARD_SUMMARY_ERROR("10005", "Unable to generate dashboard summary. Please refresh and try again.",
            HttpStatus.INTERNAL_SERVER_ERROR),
    DASHBOARD_PERMISSION_DENIED("10006",
            "You do not have permission to access this dashboard. Contact your administrator.", HttpStatus.FORBIDDEN),
    DASHBOARD_INVALID_DATE_RANGE("10007",
            "Invalid date range provided for dashboard. Start date must be before end date.", HttpStatus.BAD_REQUEST),

    USER_NOT_FOUND("20001", "The requested user does not exist. Please verify the user ID and try again.",
            HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("20002",
            "A user with this email address already exists in the system. Please use a different email.",
            HttpStatus.CONFLICT),
    INVALID_USER_INPUT("20003",
            "One or more user input fields are invalid. Please check your input and try again. Common issues: invalid email format, password too short, name too long.",
            HttpStatus.BAD_REQUEST),
    USER_INACTIVE("20004",
            "This user account is inactive and cannot access the system. Please contact your administrator to reactivate.",
            HttpStatus.FORBIDDEN),
    USER_CREATION_FAILED("20005",
            "Failed to create new user. Please try again or contact support if the issue persists.",
            HttpStatus.INTERNAL_SERVER_ERROR),
    USER_UPDATE_FAILED("20006", "Failed to update user information. Please try again or contact support.",
            HttpStatus.INTERNAL_SERVER_ERROR),
    USER_DELETION_FAILED("20007",
            "Failed to delete user. The user may be associated with important records. Contact support for assistance.",
            HttpStatus.INTERNAL_SERVER_ERROR),
    USER_BATCH_OPERATION_FAILED("20008",
            "Batch operation on users failed partially. Some users were processed successfully, others failed.",
            HttpStatus.INTERNAL_SERVER_ERROR),
    DUPLICATE_EMAIL("20009", "The provided email is already in use by another user. Please use a unique email address.",
            HttpStatus.CONFLICT),
    INVALID_EMAIL_FORMAT("20010",
            "The email address provided is invalid. Please provide a valid email format (e.g., user@example.com).",
            HttpStatus.BAD_REQUEST),
    PASSWORD_TOO_WEAK("20011",
            "Password does not meet security requirements. Password must be 8-50 characters with uppercase, lowercase, and numbers.",
            HttpStatus.BAD_REQUEST),
    USER_PROFILE_INCOMPLETE("20012", "User profile is incomplete. Please complete all required fields.",
            HttpStatus.BAD_REQUEST),

    FINANCIAL_RECORD_NOT_FOUND("30001",
            "The requested financial record does not exist. Please verify the record ID and try again.",
            HttpStatus.NOT_FOUND),
    FINANCIAL_RECORD_ALREADY_EXISTS("30002",
            "A financial record with the same details already exists. Please check for duplicates.",
            HttpStatus.CONFLICT),
    INVALID_FINANCIAL_RECORD_INPUT("30003",
            "One or more financial record fields are invalid. Please check amount, date, category, and other fields.",
            HttpStatus.BAD_REQUEST),
    INVALID_RECORD_AMOUNT("30004", "Invalid amount provided. Amount must be greater than zero.",
            HttpStatus.BAD_REQUEST),
    INVALID_RECORD_DATE("30005", "Invalid date provided. Date cannot be in the future or before system start date.",
            HttpStatus.BAD_REQUEST),
    INVALID_RECORD_CATEGORY("30006", "The provided category is not valid. Please select from available categories.",
            HttpStatus.BAD_REQUEST),
    FINANCIAL_RECORD_CREATION_FAILED("30007",
            "Failed to create new financial record. Please try again or contact support.",
            HttpStatus.INTERNAL_SERVER_ERROR),
    FINANCIAL_RECORD_UPDATE_FAILED("30008",
            "Failed to update financial record. Please verify your changes and try again.",
            HttpStatus.INTERNAL_SERVER_ERROR),
    FINANCIAL_RECORD_DELETION_FAILED("30009",
            "Failed to delete financial record. Record may be locked or referenced by other records.",
            HttpStatus.INTERNAL_SERVER_ERROR),
    FINANCIAL_RECORD_FETCH_FAILED("30010", "Failed to retrieve financial records. Please try again later.",
            HttpStatus.INTERNAL_SERVER_ERROR),
    RECORD_AMOUNT_EXCEEDS_LIMIT("30011",
            "The record amount exceeds the allowed limit. Please reduce the amount and try again.",
            HttpStatus.BAD_REQUEST),
    DUPLICATE_RECORD_DETECTED("30012",
            "A similar financial record already exists on the same date with the same amount. Check for duplicates.",
            HttpStatus.CONFLICT),
    RECORD_TYPE_MISMATCH("30013", "The record type does not match the selected category. Please verify and try again.",
            HttpStatus.BAD_REQUEST),
    FINANCIAL_RECORD_BATCH_FAILED("30014",
            "Batch import of financial records failed. Please check the file format and try again.",
            HttpStatus.BAD_REQUEST),

    UNAUTHORIZED("40001", "Authentication required. Please log in to access this resource.", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("40002", "Invalid username or password. Please check your credentials and try again.",
            HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("40003", "Your authentication token has expired. Please log in again.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("40004", "The provided authentication token is invalid or malformed. Please log in again.",
            HttpStatus.UNAUTHORIZED),
    TOKEN_REFRESH_FAILED("40005", "Failed to refresh authentication token. Please log in again.",
            HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED_ACCESS("40006",
            "You do not have permission to access this resource. Your role does not grant sufficient privileges.",
            HttpStatus.FORBIDDEN),
    FORBIDDEN("40007", "Access denied. This operation is not allowed for your account.", HttpStatus.FORBIDDEN),
    ROLE_MODIFICATION_NOT_ALLOWED("40008",
            "You do not have permission to modify user roles. Only administrators can perform this action.",
            HttpStatus.FORBIDDEN),
    INSUFFICIENT_PERMISSIONS("40009",
            "Insufficient permissions to perform this action. Please contact your administrator.",
            HttpStatus.FORBIDDEN),
    SESSION_EXPIRED("40010", "Your session has expired. Please log in again.", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED("40011",
            "Your account has been locked due to multiple failed login attempts. Please contact support.",
            HttpStatus.FORBIDDEN),
    ACCOUNT_SUSPENDED("40012", "Your account has been suspended. Please contact your administrator for assistance.",
            HttpStatus.FORBIDDEN),
    LOGIN_FAILED("40013", "Login failed. Please try again or reset your password.", HttpStatus.UNAUTHORIZED),
    PASSWORD_RESET_TOKEN_INVALID("40014",
            "The password reset token is invalid or has expired. Please request a new password reset.",
            HttpStatus.BAD_REQUEST),
    PASSWORD_RESET_FAILED("40015", "Failed to reset password. Please try again or contact support.",
            HttpStatus.INTERNAL_SERVER_ERROR),
    TWO_FACTOR_REQUIRED("40016", "Two-factor authentication is required. Please complete the verification.",
            HttpStatus.FORBIDDEN),
    TWO_FACTOR_FAILED("40017", "Two-factor authentication failed. Please try again or contact support.",
            HttpStatus.UNAUTHORIZED),

    VALIDATION_ERROR("99001", "Validation failed. Please check your input and try again.", HttpStatus.BAD_REQUEST),
    BAD_REQUEST("99002", "Bad request. The request format or parameters are invalid.", HttpStatus.BAD_REQUEST),
    DATA_ACCESS_ERROR("99003", "Database operation failed. Please try again later or contact support.",
            HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_ERROR("99999", "An unexpected internal server error occurred. Please try again later or contact support.",
            HttpStatus.INTERNAL_SERVER_ERROR);

    private final String errorCode;
    private final String errorMessage;
    private final HttpStatus httpStatus;

    /**
     * Constructor for ErrorCodeEnum
     * 
     * @param errorCode    the unique error code (1xxxx, 2xxxx, 3xxxx, 4xxxx format)
     * @param errorMessage detailed error message explaining the issue and suggested
     *                     action
     * @param httpStatus   the HTTP status code to return with the error response
     */
    ErrorCodeEnum(String errorCode, String errorMessage, HttpStatus httpStatus) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.httpStatus = httpStatus;
    }

    /**
     * Get the numeric error code
     * 
     * @return the error code (e.g., "20001")
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Get the detailed error message
     * 
     * @return the error message with explanation and suggested action
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Get the HTTP status code
     * 
     * @return the HTTP status code for this error
     */
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    // Legacy method names for backward compatibility
    public String getCode() {
        return errorCode;
    }

    public String getDefaultMessage() {
        return errorMessage;
    }
}