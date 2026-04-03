package com.financedashboard.zorvyn.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.financedashboard.zorvyn.service.impl.AuthServiceImpl;
import com.financedashboard.zorvyn.dto.*;
import com.financedashboard.zorvyn.entity.PasswordResetToken;
import com.financedashboard.zorvyn.entity.User;
import com.financedashboard.zorvyn.enums.*;
import com.financedashboard.zorvyn.exception.FinancialDashboardException;
import com.financedashboard.zorvyn.exception.UserException;
import com.financedashboard.zorvyn.repository.interfaces.PasswordResetTokenRepository;
import com.financedashboard.zorvyn.repository.interfaces.UserRepository;
import com.financedashboard.zorvyn.security.JwtService;
import com.financedashboard.zorvyn.service.interfaces.EmailService;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setup() {
        user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .password("encodedPassword")
                .role(RolesEnum.VIEWER)
                .status(UserStatusEnum.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // ✅ REGISTER TESTS
    // ─────────────────────────────────────────────────────────

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest("Test", "test@example.com", "Password1");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_shouldThrow() {
        RegisterRequest request = new RegisterRequest("Test", "test@example.com", "Password1");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));

        assertThrows(UserException.class, () -> authService.register(request));
    }

    // ─────────────────────────────────────────────────────────
    // ✅ LOGIN TESTS
    // ─────────────────────────────────────────────────────────

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest("test@example.com", "Password1");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
    }

    @Test
    void login_invalidPassword_shouldThrow() {
        LoginRequest request = new LoginRequest("test@example.com", "wrong");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThrows(FinancialDashboardException.class, () -> authService.login(request));
    }

    @Test
    void login_userNotFound_shouldThrow() {
        LoginRequest request = new LoginRequest("test@example.com", "Password1");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThrows(FinancialDashboardException.class, () -> authService.login(request));
    }

    // ─────────────────────────────────────────────────────────
    // ✅ CHANGE PASSWORD
    // ─────────────────────────────────────────────────────────

    @Test
    void changePassword_success() {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "NewPass1");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(passwordEncoder.encode(any())).thenReturn("newEncoded");

        authService.changePassword(user.getEmail(), request);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void changePassword_wrongCurrentPassword_shouldThrow() {
        ChangePasswordRequest request = new ChangePasswordRequest("wrong", "NewPass1");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThrows(FinancialDashboardException.class,
                () -> authService.changePassword(user.getEmail(), request));
    }

    // ─────────────────────────────────────────────────────────
    // ✅ FORGOT PASSWORD
    // ─────────────────────────────────────────────────────────

    @Test
    void forgotPassword_success() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));

        authService.initPasswordReset(request);

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq(user.getEmail()), any());
    }

    @Test
    void forgotPassword_userNotFound_shouldNotThrow() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("unknown@example.com");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.initPasswordReset(request));
    }

    // ─────────────────────────────────────────────────────────
    // ✅ RESET PASSWORD
    // ─────────────────────────────────────────────────────────

    @Test
    void resetPassword_success() {
        ResetPasswordRequest request = new ResetPasswordRequest("token", "NewPass1");

        PasswordResetToken token = PasswordResetToken.builder()
                .token("token")
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByTokenAndUsedFalse("token"))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode(any())).thenReturn("encoded");

        authService.resetPassword(request);

        verify(userRepository).save(any(User.class));
        verify(passwordResetTokenRepository).save(token);
    }

    @Test
    void resetPassword_expiredToken_shouldThrow() {
        ResetPasswordRequest request = new ResetPasswordRequest("token", "NewPass1");

        PasswordResetToken token = PasswordResetToken.builder()
                .token("token")
                .user(user)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByTokenAndUsedFalse("token"))
                .thenReturn(Optional.of(token));

        assertThrows(FinancialDashboardException.class,
                () -> authService.resetPassword(request));
    }

}