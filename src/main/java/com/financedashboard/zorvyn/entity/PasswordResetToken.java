package com.financedashboard.zorvyn.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stores one-time password reset tokens issued by POST /v1/auth/forgot-password.
 *
 * Token lifecycle:
 *  1. Created with used=false and expiresAt = now + 15 minutes.
 *  2. All previous unused tokens for the same user are deleted before a new one is created.
 *  3. On successful password reset → used is set to true (token is invalidated).
 *  4. Expired or already-used tokens are rejected by the service.
 *
 * In production: the token UUID is emailed to the user.
 * In local dev: the token and reset URL are logged to the console.
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Cryptographically random UUID — single-use, time-limited. */
    @Column(nullable = false, unique = true)
    private String token;

    /** The user this reset token belongs to. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** When this token expires — set to now() + 15 minutes at creation time. */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Whether the token has been consumed.
     * Once used, the token cannot be reused even if it has not expired.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
