package com.financedashboard.zorvyn.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.financedashboard.zorvyn.dto.AuthResponse;
import com.financedashboard.zorvyn.dto.LoginRequest;
import com.financedashboard.zorvyn.dto.RegisterRequest;
import com.financedashboard.zorvyn.service.interfaces.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Public authentication endpoints — no JWT required.
 *
 * POST /v1/auth/register  → Creates a VIEWER account, returns JWT
 * POST /v1/auth/login     → Validates credentials, returns JWT
 *
 * Google OAuth2 login is handled separately by Spring Security at:
 * GET /oauth2/authorization/google  →  Google consent  →  /login/oauth2/code/google
 * The OAuth2LoginSuccessHandler then issues a JWT and redirects to the frontend.
 */
@Slf4j
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /v1/auth/register
     * Registers a new user with default VIEWER role.
     * Returns 201 CREATED with the JWT and user profile.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request for email={}", request.getEmail());
        AuthResponse response = authService.register(request);
        log.info("Registration successful for userId={}", response.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /v1/auth/login
     * Authenticates with email and password.
     * Returns 200 OK with the JWT and user profile.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email={}", request.getEmail());
        AuthResponse response = authService.login(request);
        log.info("Login successful for userId={}", response.getUserId());
        return ResponseEntity.ok(response);
    }
}
