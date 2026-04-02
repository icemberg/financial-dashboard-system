package com.financedashboard.zorvyn.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.financedashboard.zorvyn.entity.User;
import com.financedashboard.zorvyn.enums.RolesEnum;
import com.financedashboard.zorvyn.enums.UserStatusEnum;
import com.financedashboard.zorvyn.repository.interfaces.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles successful Google OAuth2 login.
 *
 * Flow:
 *   1. Extract email + name from Google's OAuth2User principal
 *   2. Provision a new User (VIEWER role) if first-time login, or load existing user
 *   3. Verify the account is ACTIVE
 *   4. Issue our own JWT (same format as password-based login)
 *   5. Redirect to the configured frontend URL with the token as a query param
 *
 * This ensures the frontend always works with our JWT regardless of auth method.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");

        // Google guarantees email_verified=true for standard accounts, but we check for safety
        Boolean emailVerified = oAuth2User.getAttribute("email_verified");
        if (email == null || Boolean.FALSE.equals(emailVerified)) {
            log.warn("Google OAuth2 login rejected — email not verified or missing");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Google email not verified");
            return;
        }

        log.info("Google OAuth2 login for email={}", email);

        // Provision or load the user account
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    log.info("Provisioning new Google user: email={}, name={}", email, name);
                    return userRepository.save(User.builder()
                            .email(email)
                            .name(name != null ? name : email) // fallback if name not provided
                            .password(null)                    // Google users have no local password
                            .role(RolesEnum.VIEWER)            // Principle of least privilege
                            .status(UserStatusEnum.ACTIVE)
                            .createdAt(LocalDateTime.now())
                            .build());
                });

        // Sync name from Google if it changed
        if (name != null && !name.equals(user.getName())) {
            user.setName(name);
            user.setUpdatedAt(LocalDateTime.now());
            user = userRepository.save(user);
        }

        // Block inactive accounts
        if (user.getStatus() != UserStatusEnum.ACTIVE) {
            log.warn("Google login blocked — user account inactive: email={}", email);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Your account is inactive. Contact support.");
            return;
        }

        // Generate JWT and redirect to frontend
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtService.generateToken(userDetails);

        String targetUrl = redirectUri + "?token=" + token;
        log.info("Google OAuth2 login successful for email={}, redirecting to frontend", email);
        response.sendRedirect(targetUrl);
    }
}
