package com.financedashboard.zorvyn.service.interfaces;

import com.financedashboard.zorvyn.dto.AuthResponse;
import com.financedashboard.zorvyn.dto.ChangePasswordRequest;
import com.financedashboard.zorvyn.dto.ForgotPasswordRequest;
import com.financedashboard.zorvyn.dto.LoginRequest;
import com.financedashboard.zorvyn.dto.RegisterRequest;
import com.financedashboard.zorvyn.dto.ResetPasswordRequest;

/**
 * Contract for all authentication operations.
 * Identity is always resolved server-side from credentials or JWT — never from client-supplied IDs.
 */
public interface AuthService {

    /**
     * Registers a new user with VIEWER role.
     * Validates password strength, email uniqueness, BCrypt-encodes, returns JWT.
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates a user with email and password.
     * Rejects inactive accounts and Google-only accounts (null password).
     */
    AuthResponse login(LoginRequest request);

    /**
     * Issues a fresh JWT for the already-authenticated user.
     * The caller must hold a valid (non-expired) JWT — the filter validates it before the controller runs.
     * Use this to extend a session without re-entering credentials.
     *
     * @param userEmail extracted from the existing JWT via the SecurityContext
     */
    AuthResponse refresh(String userEmail);

    /**
     * Changes the authenticated user's own password.
     * Requires the current password for verification before accepting the new one.
     * Google-only users (null password) cannot use this endpoint.
     *
     * @param userEmail extracted from JWT
     * @param request   currentPassword + newPassword
     */
    void changePassword(String userEmail, ChangePasswordRequest request);

    /**
     * Initiates a password reset for the given email.
     * Generates a UUID token (valid 15 minutes, single-use) and logs/emails it.
     * Always returns 200 regardless of whether the email exists (prevents user enumeration).
     *
     * @param request contains the email address
     */
    void initPasswordReset(ForgotPasswordRequest request);

    /**
     * Completes the password reset flow using the token from initPasswordReset.
     * Validates: token exists, not used, not expired.
     * BCrypt-encodes and persists the new password, then marks the token as used.
     *
     * @param request tokenUUID + newPassword
     */
    void resetPassword(ResetPasswordRequest request);
}
