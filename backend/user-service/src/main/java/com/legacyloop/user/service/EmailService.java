package com.legacyloop.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends the three transactional emails.
 *
 * <p>With {@code legacyloop.mail.enabled=false} — the default, and what the demo runs on — the
 * message is logged instead of sent, so the OTP is visible in the service log and nobody needs an
 * SMTP account to try the flow. The original built HTML templates for the same three messages.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${legacyloop.mail.enabled:false}")
    private boolean enabled;

    @Value("${legacyloop.mail.from:no-reply@legacyloop.local}")
    private String from;

    @Value("${legacyloop.app-url:http://localhost:5173}")
    private String appUrl;

    @Async
    public void sendVerificationCode(String to, String code, long validityMinutes) {
        send(to, "Verify your LegacyLoop email",
                """
                Welcome to LegacyLoop.

                Your verification code is %s. It expires in %d minutes.

                If you did not create an account, you can ignore this email.
                """.formatted(code, validityMinutes));
    }

    @Async
    public void sendPasswordResetLink(String to, String token, long validityMinutes) {
        send(to, "Reset your LegacyLoop password",
                """
                Use the link below to choose a new password. It expires in %d minutes.

                %s/reset-password?token=%s

                If you did not ask for this, no action is needed.
                """.formatted(validityMinutes, appUrl, token));
    }

    @Async
    public void sendTemporaryPassword(String to, String temporaryPassword) {
        send(to, "Your LegacyLoop account",
                """
                An account has been created for you.

                Temporary password: %s

                Sign in at %s and you will be asked to choose your own password.
                """.formatted(temporaryPassword, appUrl));
    }

    /**
     * The three methods above are {@code @Async} — they are called from another bean, so the proxy
     * applies and a slow mail server never delays the HTTP response that triggered it.
     */
    private void send(String to, String subject, String body) {
        if (!enabled) {
            log.info("[mail disabled] to={} subject={}\n{}", to, subject, body);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Sent '{}' to {}", subject, to);
        } catch (Exception ex) {
            log.error("Could not send '{}' to {}: {}", subject, to, ex.getMessage());
        }
    }
}
