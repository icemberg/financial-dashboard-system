package com.financedashboard.zorvyn.service.util;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility service for extracting and validating authentication details.
 * Responsible for handling authentication-related concerns outside of controllers.
 * Follows Single Responsibility Principle by isolating authentication logic.
 */
@Slf4j
@Component
public class AuthenticationHelper {

    /**
     * Safely extracts user email (principal name) from Authentication object.
     * Handles null authentication and logs unauthenticated access attempts.
     *
     * @param authentication the Spring Security Authentication object
     * @return user email if authenticated, null otherwise
     */
    public String extractUserEmail(Authentication authentication) {
        if (authentication == null) {
            log.warn("Unauthenticated access attempt detected");
            return null;
        }
        String userEmail = authentication.getName();
        log.info("Extracted user email from authentication: {}", userEmail);
        return userEmail;
    }

    /**
     * Validates that authentication is present and returns the user email.
     * Throws exception if authentication is null or email is empty.
     *
     * @param authentication the Spring Security Authentication object
     * @return user email
     * @throws IllegalArgumentException if authentication is missing or invalid
     */
    public String extractUserEmailOrThrow(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication is required but was null");
        }
        String userEmail = authentication.getName();
        if (userEmail == null || userEmail.isBlank()) {
            throw new IllegalArgumentException("User email cannot be extracted from authentication");
        }
        log.info("Validated user email from authentication: {}", userEmail);
        return userEmail;
    }
}
