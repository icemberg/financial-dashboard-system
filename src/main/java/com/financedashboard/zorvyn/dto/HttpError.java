package com.financedashboard.zorvyn.dto;

import java.time.LocalDateTime;

import com.financedashboard.zorvyn.enums.ErrorCodeEnum;

/**
 * Deprecated wrapper for legacy usages. Prefer {@link ErrorResponse}.
 */
@Deprecated
public class HttpError extends ErrorResponse {

    public HttpError() {
        super();
    }

    public HttpError(LocalDateTime timestamp, int status, String error, String message, ErrorCodeEnum code) {
        super(timestamp, status, error, message, code);
    }
}