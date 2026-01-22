package com.pfwa.service;

import com.pfwa.config.AppProperties;
import com.pfwa.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Service for sending emails (verification, password reset, notifications).
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    public EmailService(JavaMailSender mailSender, AppProperties appProperties) {
        this.mailSender = mailSender;
        this.appProperties = appProperties;
    }

    /**
     * Sends an email verification link to the user.
     */
    @Async
    public void sendVerificationEmail(User user, String token) {
        String verificationLink = buildVerificationLink(token);
        String subject = "Verify your PFWA account";
        String htmlContent = buildVerificationEmailContent(user, verificationLink);

        sendEmail(user.getEmail(), subject, htmlContent);
        logger.info("Verification email sent to {}", maskEmail(user.getEmail()));
    }

    /**
     * Sends a password reset link to the user.
     */
    @Async
    public void sendPasswordResetEmail(User user, String token) {
        String resetLink = buildPasswordResetLink(token);
        String subject = "Reset your PFWA password";
        String htmlContent = buildPasswordResetEmailContent(user, resetLink);

        sendEmail(user.getEmail(), subject, htmlContent);
        logger.info("Password reset email sent to {}", maskEmail(user.getEmail()));
    }

    /**
     * Sends a password change confirmation email.
     */
    @Async
    public void sendPasswordChangeConfirmation(User user, String ipAddress) {
        String subject = "Your PFWA password was changed";
        String htmlContent = buildPasswordChangeConfirmationContent(user, ipAddress);

        sendEmail(user.getEmail(), subject, htmlContent);
        logger.info("Password change confirmation sent to {}", maskEmail(user.getEmail()));
    }

    /**
     * Sends a security alert when all sessions are logged out.
     */
    @Async
    public void sendAllSessionsLogoutAlert(User user) {
        String subject = "Security Alert: All sessions logged out";
        String htmlContent = buildAllSessionsLogoutAlertContent(user);

        sendEmail(user.getEmail(), subject, htmlContent);
        logger.info("All sessions logout alert sent to {}", maskEmail(user.getEmail()));
    }

    /**
     * Sends an email with the given content.
     */
    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(appProperties.getEmail().getFrom(), appProperties.getEmail().getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.debug("Email sent successfully to {}", maskEmail(to));
        } catch (MailException | MessagingException | java.io.UnsupportedEncodingException e) {
            logger.error("Failed to send email to {}: {}", maskEmail(to), e.getMessage());
            // Don't throw - email failures shouldn't break the main flow
        }
    }

    private String buildVerificationLink(String token) {
        return appProperties.getFrontendUrl() + "/verify?token=" + token;
    }

    private String buildPasswordResetLink(String token) {
        return appProperties.getFrontendUrl() + "/reset-password?token=" + token;
    }

    private String buildVerificationEmailContent(User user, String link) {
        String greeting = user.getFirstName() != null ?
                "Hi " + user.getFirstName() + "," :
                "Hi,";

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #4CAF50;
                              color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Verify Your Email Address</h2>
                    <p>%s</p>
                    <p>Thank you for registering with PFWA - Personal Finance Web App. Please verify your email address by clicking the button below:</p>
                    <a href="%s" class="button">Verify Email</a>
                    <p>Or copy and paste this link into your browser:</p>
                    <p><a href="%s">%s</a></p>
                    <p>This link will expire in %d hours.</p>
                    <p>If you did not create an account with PFWA, please ignore this email.</p>
                    <div class="footer">
                        <p>This is an automated message from PFWA. Please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                greeting,
                link,
                link,
                link,
                appProperties.getEmail().getVerificationExpirationHours()
        );
    }

    private String buildPasswordResetEmailContent(User user, String link) {
        String greeting = user.getFirstName() != null ?
                "Hi " + user.getFirstName() + "," :
                "Hi,";

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #2196F3;
                              color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .warning { background-color: #FFF3CD; border: 1px solid #FFC107;
                               padding: 10px; border-radius: 4px; margin: 20px 0; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Reset Your Password</h2>
                    <p>%s</p>
                    <p>We received a request to reset your password. Click the button below to create a new password:</p>
                    <a href="%s" class="button">Reset Password</a>
                    <p>Or copy and paste this link into your browser:</p>
                    <p><a href="%s">%s</a></p>
                    <div class="warning">
                        <strong>Important:</strong> This link will expire in %d hour(s). After resetting your password, you will be logged out of all devices.
                    </div>
                    <p>If you did not request a password reset, please ignore this email. Your password will remain unchanged.</p>
                    <div class="footer">
                        <p>This is an automated message from PFWA. Please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                greeting,
                link,
                link,
                link,
                appProperties.getEmail().getPasswordResetExpirationHours()
        );
    }

    private String buildPasswordChangeConfirmationContent(User user, String ipAddress) {
        String greeting = user.getFirstName() != null ?
                "Hi " + user.getFirstName() + "," :
                "Hi,";

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .alert { background-color: #D4EDDA; border: 1px solid #28A745;
                             padding: 10px; border-radius: 4px; margin: 20px 0; }
                    .warning { background-color: #F8D7DA; border: 1px solid #DC3545;
                               padding: 10px; border-radius: 4px; margin: 20px 0; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Password Changed Successfully</h2>
                    <p>%s</p>
                    <div class="alert">
                        Your password was successfully changed.
                    </div>
                    <p><strong>Details:</strong></p>
                    <ul>
                        <li>Time: %s</li>
                        <li>IP Address: %s</li>
                    </ul>
                    <p>You have been logged out of all devices. Please log in with your new password.</p>
                    <div class="warning">
                        <strong>Didn't make this change?</strong> If you did not change your password, your account may be compromised. Please contact support immediately.
                    </div>
                    <div class="footer">
                        <p>This is an automated message from PFWA. Please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                greeting,
                java.time.ZonedDateTime.now().toString(),
                ipAddress != null ? ipAddress : "Unknown"
        );
    }

    private String buildAllSessionsLogoutAlertContent(User user) {
        String greeting = user.getFirstName() != null ?
                "Hi " + user.getFirstName() + "," :
                "Hi,";

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .alert { background-color: #FFF3CD; border: 1px solid #FFC107;
                             padding: 10px; border-radius: 4px; margin: 20px 0; }
                    .footer { margin-top: 30px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Security Alert: All Sessions Logged Out</h2>
                    <p>%s</p>
                    <div class="alert">
                        All sessions for your PFWA account have been terminated.
                    </div>
                    <p><strong>Time:</strong> %s</p>
                    <p>If you initiated this action, no further action is required. Simply log in again on any device you wish to use.</p>
                    <p>If you did not initiate this action, please change your password immediately and review your account activity.</p>
                    <div class="footer">
                        <p>This is an automated message from PFWA. Please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(
                greeting,
                java.time.ZonedDateTime.now().toString()
        );
    }

    /**
     * Masks an email address for logging (e.g., u***@example.com).
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 1) {
            return "***" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
