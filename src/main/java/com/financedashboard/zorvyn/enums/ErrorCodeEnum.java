package com.financedashboard.zorvyn.enums;

import org.springframework.http.HttpStatus;

public enum ErrorCodeEnum {
    USER_NOT_FOUND("USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND),
    VALIDATION_ERROR("VALIDATION_ERROR", "Validation failed", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("UNAUTHORIZED", "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "Forbidden", HttpStatus.FORBIDDEN),
    DATA_ACCESS_ERROR("DATA_ACCESS_ERROR", "Data access error", HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    BAD_REQUEST("BAD_REQUEST", "Bad request", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCodeEnum(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}