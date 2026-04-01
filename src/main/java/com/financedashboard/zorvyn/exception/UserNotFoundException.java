package com.financedashboard.zorvyn.exception;

import org.springframework.http.HttpStatus;

import com.financedashboard.zorvyn.enums.ErrorCodeEnum;

/**
 * Deprecated: use FinancialDashboardException directly with ErrorCodeEnum.USER_NOT_FOUND
 */
@Deprecated
public class UserNotFoundException extends FinancialDashboardException {

    public UserNotFoundException(String message) {
        super(message, ErrorCodeEnum.USER_NOT_FOUND, HttpStatus.NOT_FOUND);
    }
}