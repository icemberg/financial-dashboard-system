package com.financedashboard.zorvyn.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.financedashboard.zorvyn.enums.ErrorCodeEnum;
import com.financedashboard.zorvyn.exception.FinancialDashboardException;
import com.financedashboard.zorvyn.service.interfaces.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SMTP-backed implementation of {@link EmailService}.
 *
 * <p>Uses Spring Boot's auto-configured {@link JavaMailSender} (backed by
 * {@code spring.mail.*} properties) to deliver transactional emails such as
 * password-reset links.</p>
 *
 * <p>The sender address and base URL for reset links are read from
 * {@code app.mail.*} properties so they can differ across profiles.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from-address}")
    private String fromAddress;

    @Value("${app.mail.password-reset-base-url}")
    private String passwordResetBaseUrl;

    /**
     * Sends an HTML password-reset email containing a clickable reset link.
     * The link points to the frontend reset page, appending the token as a query parameter.
     *
     * @param toEmail    recipient address
     * @param resetToken UUID token the user must submit to complete the reset
     */
    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        log.info("Sending password reset email to {}", toEmail);

        String resetLink = passwordResetBaseUrl + "?token=" + resetToken;

        String subject = "Zorvyn — Password Reset Request";

        String htmlBody = """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 560px; margin: auto;
                            padding: 32px; background: #fafafa; border-radius: 12px;">
                    <h2 style="color: #1a1a2e;">Password Reset</h2>
                    <p style="color: #444; line-height: 1.6;">
                        We received a request to reset your password. Click the button below to choose a new one.
                        This link is valid for <strong>15 minutes</strong>.
                    </p>
                    <a href="%s"
                       style="display: inline-block; margin: 24px 0; padding: 12px 28px;
                              background: #4361ee; color: #fff; text-decoration: none;
                              border-radius: 8px; font-weight: 600;">
                        Reset Password
                    </a>
                    <p style="color: #888; font-size: 13px; line-height: 1.5;">
                        If you did not request this, you can safely ignore this email.
                        Your password will remain unchanged.
                    </p>
                    <hr style="border: none; border-top: 1px solid #e0e0e0; margin: 24px 0;">
                    <p style="color: #aaa; font-size: 12px;">
                        Zorvyn Finance Dashboard &mdash; Automated message, please do not reply.
                    </p>
                </div>
                """.formatted(resetLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true → interpret as HTML

            mailSender.send(message);
            log.info("Password reset email sent successfully to {}", toEmail);
        } catch (MailException | MessagingException ex) {
            log.error("Failed to send password reset email to {}", toEmail, ex);
            throw new FinancialDashboardException(
                    ErrorCodeEnum.PASSWORD_RESET_FAILED.getErrorCode(),
                    "Unable to send password reset email. Please try again later.",
                    ErrorCodeEnum.PASSWORD_RESET_FAILED.getHttpStatus()
            );
        }
    }
}
