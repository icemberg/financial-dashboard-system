package com.financedashboard.zorvyn.service.impl;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.financedashboard.zorvyn.dto.AuthResponse;
import com.financedashboard.zorvyn.dto.LoginRequest;
import com.financedashboard.zorvyn.dto.RegisterRequest;
import com.financedashboard.zorvyn.entity.User;
import com.financedashboard.zorvyn.enums.ErrorCodeEnum;
import com.financedashboard.zorvyn.enums.RolesEnum;
import com.financedashboard.zorvyn.enums.UserStatusEnum;
import com.financedashboard.zorvyn.exception.FinancialDashboardException;
import com.financedashboard.zorvyn.exception.UserException;
import com.financedashboard.zorvyn.repository.interfaces.UserRepository;
import com.financedashboard.zorvyn.security.CustomUserDetails;
import com.financedashboard.zorvyn.security.JwtService;
import com.financedashboard.zorvyn.service.interfaces.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles user registration and password-based login.
 *
 * Security principles applied:
 * - Passwords are BCrypt-encoded before storage
 * - Inactive users are rejected before any token is issued
 * - Generic error messages prevent user enumeration on login
 * - User identity is always derived from credentials, never from client-supplied IDs
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Registers a new user.
     *
     * 1. Validate password strength (service-layer check beyond @Size)
     * 2. Check for duplicate email → 409 CONFLICT
     * 3. Encode password with BCrypt
     * 4. Save with role=VIEWER (least privilege default), status=ACTIVE
     * 5. Generate and return JWT
     */
    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("Registration attempt for email={}", request.getEmail());

        // Service-layer password strength: uppercase + lowercase + digit required
        validatePasswordStrength(request.getPassword());

        // Check for duplicate email
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Registration failed — email already exists: {}", request.getEmail());
            throw new UserException(
                    ErrorCodeEnum.USER_ALREADY_EXISTS.getErrorCode(),
                    ErrorCodeEnum.USER_ALREADY_EXISTS.getErrorMessage() + ": " + request.getEmail(),
                    ErrorCodeEnum.USER_ALREADY_EXISTS.getHttpStatus()
            );
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(RolesEnum.VIEWER)           // Self-registered users always start as VIEWER
                .status(UserStatusEnum.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: id={}, email={}", user.getId(), user.getEmail());

        return buildAuthResponse(user);
    }

    /**
     * Authenticates a user with email and password.
     *
     * 1. Load user by email (generic error to prevent enumeration)
     * 2. Verify account is ACTIVE
     * 3. Verify BCrypt password match
     * 4. Generate and return JWT
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email={}", request.getEmail());

        // Generic "invalid credentials" message to prevent email enumeration
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed — email not found: {}", request.getEmail());
                    return new FinancialDashboardException(
                            ErrorCodeEnum.INVALID_CREDENTIALS.getErrorCode(),
                            ErrorCodeEnum.INVALID_CREDENTIALS.getErrorMessage(),
                            ErrorCodeEnum.INVALID_CREDENTIALS.getHttpStatus()
                    );
                });

        // Reject inactive accounts before even checking the password
        if (user.getStatus() != UserStatusEnum.ACTIVE) {
            log.warn("Login blocked — account inactive: email={}", request.getEmail());
            throw new FinancialDashboardException(
                    ErrorCodeEnum.USER_INACTIVE.getErrorCode(),
                    ErrorCodeEnum.USER_INACTIVE.getErrorMessage(),
                    ErrorCodeEnum.USER_INACTIVE.getHttpStatus()
            );
        }

        // Google-only users (null password) cannot log in with password
        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed — invalid password for email={}", request.getEmail());
            throw new FinancialDashboardException(
                    ErrorCodeEnum.INVALID_CREDENTIALS.getErrorCode(),
                    ErrorCodeEnum.INVALID_CREDENTIALS.getErrorMessage(),
                    ErrorCodeEnum.INVALID_CREDENTIALS.getHttpStatus()
            );
        }

        log.info("Login successful for email={}", request.getEmail());
        return buildAuthResponse(user);
    }

    /** Builds the AuthResponse from a User entity and a fresh JWT. */
    private AuthResponse buildAuthResponse(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMs() / 1000) // seconds
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .build();
    }

    /**
     * Validates password strength beyond @Size constraints.
     * Requirement: at least one uppercase, one lowercase, one digit.
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
