package com.financedashboard.zorvyn.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.financedashboard.zorvyn.dto.AuthResponse;
import com.financedashboard.zorvyn.dto.ChangePasswordRequest;
import com.financedashboard.zorvyn.dto.ErrorResponse;
import com.financedashboard.zorvyn.dto.ForgotPasswordRequest;
import com.financedashboard.zorvyn.dto.LoginRequest;
import com.financedashboard.zorvyn.dto.RegisterRequest;
import com.financedashboard.zorvyn.dto.ResetPasswordRequest;
import com.financedashboard.zorvyn.service.interfaces.AuthService;
import com.financedashboard.zorvyn.service.util.AuthenticationHelper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Authentication and credential-management endpoints.
 *
 * Public (no JWT required):
 *   POST /v1/auth/register          → Register as VIEWER, returns JWT
 *   POST /v1/auth/login             → Email/password login, returns JWT
 *   POST /v1/auth/forgot-password   → Initiate password reset (sends token)
 *   POST /v1/auth/reset-password    → Complete reset with token + new password
 *
 * Protected (valid JWT required):
 *   POST  /v1/auth/refresh          → Exchange a valid JWT for a fresh one
 *   PATCH /v1/auth/change-password  → Change own password (must know current)
 *
 * Google OAuth2 is handled separately by Spring Security:
 *   GET /oauth2/authorization/google → Google consent → OAuth2LoginSuccessHandler → JWT redirect
 */
@Slf4j
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, token refresh, and password management")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationHelper authenticationHelper;

    // ── Public Endpoints ──────────────────────────────────────────────────────

    @Operation(
            summary = "Register a new user",
            description = "Creates a new account with VIEWER role. Returns a JWT token and user profile. "
                    + "Email must be unique. Password must contain at least 1 uppercase, 1 lowercase, and 1 digit."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or password too weak",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request for email={}", request.getEmail());
        AuthResponse response = authService.register(request);
        log.info("Registration successful: userId={}", response.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Login with email and password",
            description = "Authenticates the user and returns a JWT token. "
                    + "Uses the same error message for unknown email and wrong password to prevent user enumeration. "
                    + "Google-only accounts (no local password) receive 401."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Account is inactive",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email={}", request.getEmail());
        AuthResponse response = authService.login(request);
        log.info("Login successful: userId={}", response.getUserId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Initiate password reset",
            description = "Generates a single-use UUID token with a 15-minute TTL. "
                    + "In local dev, the token is logged to the server console. "
                    + "Always returns 200 regardless of whether the email exists — prevents user enumeration."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reset initiated (always returns 200)")
    })
    @SecurityRequirement(name = "")
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot-password request for email={}", request.getEmail());
        authService.initPasswordReset(request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Complete password reset",
            description = "Resets the password using the token received from forgot-password. "
                    + "Token must be unused and not expired. New password must meet strength requirements."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Token invalid, expired, or password too weak",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "")
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset-password execution attempt");
        authService.resetPassword(request);
        log.info("Password reset completed successfully");
        return ResponseEntity.noContent().build();
    }

    // ── Protected Endpoints (valid JWT required) ──────────────────────────────

    @Operation(
            summary = "Refresh JWT token",
            description = "Issues a fresh JWT for the currently authenticated user. "
                    + "The existing token must still be valid. Use this to extend a session before expiry."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Account became inactive",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(Authentication authentication) {
        String userEmail = authenticationHelper.extractUserEmailOrThrow(authentication);
        log.info("Token refresh for email={}", userEmail);
        AuthResponse response = authService.refresh(userEmail);
        log.info("Token refreshed for email={}", userEmail);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Change own password",
            description = "Changes the authenticated user's password. Requires the current password for verification. "
                    + "Google-only accounts (null password) are rejected with 401."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "New password too weak",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Current password incorrect or Google-only account",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        String userEmail = authenticationHelper.extractUserEmailOrThrow(authentication);
        log.info("Change-password request for email={}", userEmail);
        authService.changePassword(userEmail, request);
        log.info("Password changed successfully for email={}", userEmail);
        return ResponseEntity.noContent().build();
    }
}
