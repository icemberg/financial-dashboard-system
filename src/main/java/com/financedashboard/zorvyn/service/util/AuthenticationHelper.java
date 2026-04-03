package com.financedashboard.zorvyn.service.util;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.financedashboard.zorvyn.enums.ErrorCodeEnum;
import com.financedashboard.zorvyn.exception.FinancialDashboardException;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility for extracting and validating the authenticated user's email from the
 * SecurityContext.
 *
 * Bug #4 fix: extractUserEmailOrThrow() previously threw unchecked
 * IllegalArgumentException,
 * which bypassed GlobalExceptionHandler and returned a raw 500. It now throws
 * FinancialDashboardException with UNAUTHORIZED (401), which is handled
 * correctly.
 */
@Slf4j
@Component
public class AuthenticationHelper {

    /**
     * Safely extracts the user's email (principal name) from the Authentication
     * object.
     * Returns null if not authenticated — callers that need a guaranteed email
     * should
     * use extractUserEmailOrThrow() instead.
     *
     * @param authentication the Spring Security Authentication from the
     *                       SecurityContext
     * @return user email, or null if authentication is absent
     */
    public String extractUserEmail(Authentication authentication) {
        if (authentication == null) {
            log.warn("Unauthenticated access attempt detected");
            return null;
        }
        String userEmail = authentication.getName();
        log.debug("Extracted user email from authentication: {}", userEmail);
        return userEmail;
    }

    /**
     * Extracts the user's email and throws a structured 401 if authentication is missing/invalid.
     *
     * Bug #4 fix: throws FinancialDashboardException (handled by GlobalExceptionHandler → 401)
     * instead of the previous IllegalArgumentException (unhandled → raw 500).
     *
     * @param authentication the Spring Security Authentication from the SecurityContext
     * @return user email (guaranteed non-null, non-blank)
     * @throws FinancialDashboardException 401 UNAUTHORIZED if authentication is null or email is blank
     */
    public String extractUserEmailOrThrow(Authentication authentication) {
        if (authentication == null) {
            log.warn("Authentication object is null — rejecting request with 401");
            throw new FinancialDashboardException(
                    ErrorCodeEnum.UNAUTHORIZED.getErrorCode(),
                    ErrorCodeEnum.UNAUTHORIZED.getErrorMessage(),
                    HttpStatus.UNAUTHORIZED
            );
        }

        String userEmail = authentication.getName();

        if (userEmail == null || userEmail.isBlank()) {
            log.warn("Authentication present but email is blank — rejecting request with 401");
            throw new FinancialDashboardException(
                    ErrorCodeEnum.UNAUTHORIZED.getErrorCode(),
                    "Unable to resolve authenticated user identity.",
                    HttpStatus.UNAUTHORIZED
            );
        }

        log.debug("Validated user email from authentication: {}", userEmail);

        return userEmail;
    }
}
