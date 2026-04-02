package com.financedashboard.zorvyn.service.interfaces;

import com.financedashboard.zorvyn.dto.AuthResponse;
import com.financedashboard.zorvyn.dto.LoginRequest;
import com.financedashboard.zorvyn.dto.RegisterRequest;

/**
 * Contract for authentication operations.
 * Authentication identity is always derived server-side from credentials — never from client-supplied IDs.
 */
public interface AuthService {

    /**
     * Registers a new user with VIEWER role.
     * Validates uniqueness of email, encodes password, and returns a JWT.
     *
     * @param request registration data (name, email, password)
     * @return AuthResponse with JWT token and user profile
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates a user with email and password.
     * Verifies the account is ACTIVE and the password matches the BCrypt hash.
     *
     * @param request login credentials
     * @return AuthResponse with JWT token and user profile
     */
    AuthResponse login(LoginRequest request);
}
