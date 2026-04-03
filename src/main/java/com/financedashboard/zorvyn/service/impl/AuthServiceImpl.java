package com.financedashboard.zorvyn.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.financedashboard.zorvyn.dto.AuthResponse;
import com.financedashboard.zorvyn.dto.ChangePasswordRequest;
import com.financedashboard.zorvyn.dto.ForgotPasswordRequest;
import com.financedashboard.zorvyn.dto.LoginRequest;
import com.financedashboard.zorvyn.dto.RegisterRequest;
import com.financedashboard.zorvyn.dto.ResetPasswordRequest;
import com.financedashboard.zorvyn.entity.PasswordResetToken;
import com.financedashboard.zorvyn.entity.User;
import com.financedashboard.zorvyn.enums.ErrorCodeEnum;
import com.financedashboard.zorvyn.enums.RolesEnum;
import com.financedashboard.zorvyn.enums.UserStatusEnum;
import com.financedashboard.zorvyn.exception.FinancialDashboardException;
import com.financedashboard.zorvyn.exception.UserException;
import com.financedashboard.zorvyn.repository.interfaces.PasswordResetTokenRepository;
import com.financedashboard.zorvyn.repository.interfaces.UserRepository;
import com.financedashboard.zorvyn.security.CustomUserDetails;
import com.financedashboard.zorvyn.security.JwtService;
import com.financedashboard.zorvyn.service.interfaces.AuthService;
import com.financedashboard.zorvyn.service.interfaces.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles all authentication operations: registration, login, JWT refresh,
 * password change, and the full forgot/reset password flow.
 *
 * Security principles:
 * - Passwords are BCrypt-encoded before storage; never returned in any response.
 * - Inactive users are rejected before any token is issued.
 * - Generic error messages prevent user enumeration on login and forgot-password.
 * - User identity is always derived server-side; client-supplied IDs are ignored.
 * - Password reset tokens are single-use, 15-minute TTL, UUID-based.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    // ── Register ─────────────────────────────────────────────────────────────

    /**
     * 1. Validate password strength.
     * 2. Check for duplicate email → 409.
     * 3. BCrypt-encode password.
     * 4. Save with role=VIEWER (least privilege), status=ACTIVE.
     * 5. Return JWT.
     */
    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("Registration attempt for email={}", request.getEmail());

        validatePasswordStrength(request.getPassword());

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Registration failed — email already exists: {}", request.getEmail());
            throw new UserException(
                    ErrorCodeEnum.USER_ALREADY_EXISTS.getErrorCode(),
                    ErrorCodeEnum.USER_ALREADY_EXISTS.getErrorMessage() + ": " + request.getEmail(),
                    ErrorCodeEnum.USER_ALREADY_EXISTS.getHttpStatus()
            );
        }

        try {
            User user = User.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(RolesEnum.VIEWER)
                    .status(UserStatusEnum.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();

            user = userRepository.save(user);
            log.info("User registered: id={}, email={}", user.getId(), user.getEmail());
            return buildAuthResponse(user);
        } catch (FinancialDashboardException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during registration for email={}", request.getEmail(), ex);
            throw new FinancialDashboardException(
                    ErrorCodeEnum.USER_CREATION_FAILED.getErrorCode(),
                    ErrorCodeEnum.USER_CREATION_FAILED.getErrorMessage(),
                    ErrorCodeEnum.USER_CREATION_FAILED.getHttpStatus()
            );
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * 1. Load user by email (generic error prevents enumeration).
     * 2. Reject inactive accounts.
     * 3. Reject Google-only users (null password).
     * 4. Verify BCrypt match.
     * 5. Return JWT.
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email={}", request.getEmail());

        try {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> {
                        log.warn("Login failed — email not found: {}", request.getEmail());
                        return new FinancialDashboardException(
                                ErrorCodeEnum.INVALID_CREDENTIALS.getErrorCode(),
                                ErrorCodeEnum.INVALID_CREDENTIALS.getErrorMessage(),
                                ErrorCodeEnum.INVALID_CREDENTIALS.getHttpStatus()
                        );
                    });

            if (user.getStatus() != UserStatusEnum.ACTIVE) {
                log.warn("Login blocked — account inactive: email={}", request.getEmail());
                throw new FinancialDashboardException(
                        ErrorCodeEnum.USER_INACTIVE.getErrorCode(),
                        ErrorCodeEnum.USER_INACTIVE.getErrorMessage(),
                        ErrorCodeEnum.USER_INACTIVE.getHttpStatus()
                );
            }

            if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                log.warn("Login failed — password mismatch or Google-only account: email={}", request.getEmail());
                throw new FinancialDashboardException(
                        ErrorCodeEnum.INVALID_CREDENTIALS.getErrorCode(),
                        ErrorCodeEnum.INVALID_CREDENTIALS.getErrorMessage(),
                        ErrorCodeEnum.INVALID_CREDENTIALS.getHttpStatus()
                );
            }

            log.info("Login successful: email={}", request.getEmail());
            return buildAuthResponse(user);
        } catch (FinancialDashboardException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during login for email={}", request.getEmail(), ex);
            throw new FinancialDashboardException(
                    ErrorCodeEnum.LOGIN_FAILED.getErrorCode(),
                    ErrorCodeEnum.LOGIN_FAILED.getErrorMessage(),
                    ErrorCodeEnum.LOGIN_FAILED.getHttpStatus()
            );
        }
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    /**
     * Issues a fresh JWT for the already-authenticated caller.
     * The incoming JWT is validated by JwtAuthFilter before this method runs,
     * so we only need to reload the user and issue a new token.
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(String userEmail) {
        log.info("JWT refresh for email={}", userEmail);

        try {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new FinancialDashboardException(
                            ErrorCodeEnum.USER_NOT_FOUND.getErrorCode(),
                            ErrorCodeEnum.USER_NOT_FOUND.getErrorMessage(),
                            ErrorCodeEnum.USER_NOT_FOUND.getHttpStatus()
                    ));

            if (user.getStatus() != UserStatusEnum.ACTIVE) {
                throw new FinancialDashboardException(
                        ErrorCodeEnum.USER_INACTIVE.getErrorCode(),
                        ErrorCodeEnum.USER_INACTIVE.getErrorMessage(),
                        ErrorCodeEnum.USER_INACTIVE.getHttpStatus()
                );
            }

            log.info("JWT refreshed for email={}", userEmail);
            return buildAuthResponse(user);
        } catch (FinancialDashboardException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during token refresh for email={}", userEmail, ex);
            throw new FinancialDashboardException(
                    ErrorCodeEnum.TOKEN_REFRESH_FAILED.getErrorCode(),
                    ErrorCodeEnum.TOKEN_REFRESH_FAILED.getErrorMessage(),
                    ErrorCodeEnum.TOKEN_REFRESH_FAILED.getHttpStatus()
            );
        }
    }

    // ── Change Password ───────────────────────────────────────────────────────

    /**
     * 1. Reject Google-only users (null password).
     * 2. Verify current password matches stored BCrypt hash.
     * 3. Validate new password strength.
     * 4. BCrypt-encode and persist the new password.
     */
    @Override
    public void changePassword(String userEmail, ChangePasswordRequest request) {
        log.info("Password change attempt for email={}", userEmail);

        try {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new FinancialDashboardException(
                            ErrorCodeEnum.USER_NOT_FOUND.getErrorCode(),
                            ErrorCodeEnum.USER_NOT_FOUND.getErrorMessage(),
                            ErrorCodeEnum.USER_NOT_FOUND.getHttpStatus()
                    ));

            // Google-only accounts have no local password to change
            if (user.getPassword() == null) {
                log.warn("Password change rejected — Google-only account: email={}", userEmail);
                throw new FinancialDashboardException(
                        ErrorCodeEnum.INVALID_CREDENTIALS.getErrorCode(),
                        "Google-authenticated accounts cannot use password change. " +
                        "Use your Google account settings to manage your password.",
                        ErrorCodeEnum.INVALID_CREDENTIALS.getHttpStatus()
                );
            }

            // Verify the provided current password
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                log.warn("Password change failed — incorrect current password: email={}", userEmail);
                throw new FinancialDashboardException(
                        ErrorCodeEnum.INVALID_CREDENTIALS.getErrorCode(),
                        "Current password is incorrect.",
                        ErrorCodeEnum.INVALID_CREDENTIALS.getHttpStatus()
                );
            }

            validatePasswordStrength(request.getNewPassword());

            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("Password changed successfully for email={}", userEmail);
        } catch (FinancialDashboardException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during password change for email={}", userEmail, ex);
            throw new FinancialDashboardException(
                    ErrorCodeEnum.PASSWORD_RESET_FAILED.getErrorCode(),
                    ErrorCodeEnum.PASSWORD_RESET_FAILED.getErrorMessage(),
                    ErrorCodeEnum.PASSWORD_RESET_FAILED.getHttpStatus()
            );
        }
    }

    // ── Forgot Password ───────────────────────────────────────────────────────

    /**
     * Generates a single-use, 15-minute UUID reset token.
     * Always returns 200 — even if the email is not registered — to prevent user enumeration.
     * In local dev the token is logged to the console.
     * In production this would send an email with the reset link.
     */
    @Override
    public void initPasswordReset(ForgotPasswordRequest request) {
        log.info("Password reset requested for email={}", request.getEmail());

        try {
            Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
            if (userOpt.isEmpty()) {
                log.warn("Password reset silently ignored — email not found: {}", request.getEmail());
                return; // silently succeed — do NOT reveal that the email is not registered
            }

            User user = userOpt.get();

            // Remove any pre-existing unused tokens before creating a fresh one
            passwordResetTokenRepository.deleteByUserAndUsedFalse(user);

            String tokenValue = UUID.randomUUID().toString();

            PasswordResetToken token = PasswordResetToken.builder()
                    .token(tokenValue)
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .used(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            passwordResetTokenRepository.save(token);

            emailService.sendPasswordResetEmail(user.getEmail(), tokenValue);
            log.debug("Password reset token generated for email={}", request.getEmail());
        } catch (FinancialDashboardException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during password reset initiation for email={}", request.getEmail(), ex);
            throw new FinancialDashboardException(
                    ErrorCodeEnum.PASSWORD_RESET_FAILED.getErrorCode(),
                    ErrorCodeEnum.PASSWORD_RESET_FAILED.getErrorMessage(),
                    ErrorCodeEnum.PASSWORD_RESET_FAILED.getHttpStatus()
            );
        }
    }

    // ── Reset Password ────────────────────────────────────────────────────────

    /**
     * 1. Look up the token (must be unused).
     * 2. Reject if expired.
     * 3. Validate new password strength.
     * 4. BCrypt-encode and persist.
     * 5. Mark the token as used (single-use guarantee).
     */
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Password reset execution attempted");

        try {
            PasswordResetToken resetToken = passwordResetTokenRepository
                    .findByTokenAndUsedFalse(request.getToken())
                    .orElseThrow(() -> {
                        log.warn("Password reset failed — token not found or already used");
                        return new FinancialDashboardException(
                                ErrorCodeEnum.PASSWORD_RESET_TOKEN_INVALID.getErrorCode(),
                                ErrorCodeEnum.PASSWORD_RESET_TOKEN_INVALID.getErrorMessage(),
                                ErrorCodeEnum.PASSWORD_RESET_TOKEN_INVALID.getHttpStatus()
                        );
                    });

            if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                log.warn("Password reset failed — token expired for userId={}", resetToken.getUser().getId());
                throw new FinancialDashboardException(
                        ErrorCodeEnum.PASSWORD_RESET_TOKEN_INVALID.getErrorCode(),
                        "Password reset token has expired. Please request a new one.",
                        ErrorCodeEnum.PASSWORD_RESET_TOKEN_INVALID.getHttpStatus()
                );
            }

            validatePasswordStrength(request.getNewPassword());

            User user = resetToken.getUser();
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            // Mark as used — prevents replay attacks
            resetToken.setUsed(true);
            passwordResetTokenRepository.save(resetToken);

            log.info("Password reset successful for userId={}", user.getId());
        } catch (FinancialDashboardException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during password reset", ex);
            throw new FinancialDashboardException(
                    ErrorCodeEnum.PASSWORD_RESET_FAILED.getErrorCode(),
                    ErrorCodeEnum.PASSWORD_RESET_FAILED.getErrorMessage(),
                    ErrorCodeEnum.PASSWORD_RESET_FAILED.getHttpStatus()
            );
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /** Constructs an AuthResponse from a User entity issuing a fresh JWT. */
    private AuthResponse buildAuthResponse(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs() / 1000)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .build();
    }

    /**
     * Service-layer password strength validation beyond @Size.
     * Requires: at least one lowercase, one uppercase, one digit.
     */
    private void validatePasswordStrength(String password) {
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")) {
            throw new UserException(
                    ErrorCodeEnum.PASSWORD_TOO_WEAK.getErrorCode(),
                    ErrorCodeEnum.PASSWORD_TOO_WEAK.getErrorMessage(),
                    ErrorCodeEnum.PASSWORD_TOO_WEAK.getHttpStatus()
            );
        }
    }
}
