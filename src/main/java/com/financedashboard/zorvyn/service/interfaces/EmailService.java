package com.financedashboard.zorvyn.service.interfaces;

/**
 * Abstraction for sending outbound emails.
 * Implementations may use SMTP, a transactional email provider (SendGrid, SES), etc.
 */
public interface EmailService {

    /**
     * Sends a password-reset email containing the one-time reset token.
     *
     * @param toEmail    the recipient's email address
     * @param resetToken the UUID reset token the user must submit to {@code POST /v1/auth/reset-password}
     */
    void sendPasswordResetEmail(String toEmail, String resetToken);
}
