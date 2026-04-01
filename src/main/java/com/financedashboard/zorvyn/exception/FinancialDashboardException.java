package com.financedashboard.zorvyn.exception;

import org.springframework.http.HttpStatus;

import com.financedashboard.zorvyn.enums.ErrorCodeEnum;

public class FinancialDashboardException extends RuntimeException {

    private final ErrorCodeEnum code;
    private final HttpStatus status;

    public FinancialDashboardException(String message, ErrorCodeEnum code, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public ErrorCodeEnum getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}