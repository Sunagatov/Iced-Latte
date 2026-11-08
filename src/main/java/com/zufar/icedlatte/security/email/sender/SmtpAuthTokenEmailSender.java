package com.zufar.icedlatte.security.email.sender;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "email.enabled", havingValue = "true")
public class SmtpAuthTokenEmailSender implements AuthTokenEmailSender {

    private final JavaMailSender javaMailSender;
    private final MessageSource messageSource;

    @Value("${spring.mail.subject.confirmation}")
    private String subject;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void sendPasswordReset(String email, String token) {
        send(
                email,
                "Reset your Iced Latte password",
                "Reset your password using this link (valid for 5 minutes):\n" + frontendUrl.replaceAll("/+$", "")
                        + "/resetpass?token=" + token + "\n\nOr enter this code: " + token
                        + "\nIf you did not request this, ignore this email.");
    }

    @Override
    public void sendPasswordChanged(String email) {
        send(
                email,
                "Your Iced Latte password changed",
                "Your password has been changed and previous sessions have been signed out. If you did not make this change, reset your password immediately or contact support.");
    }

    private void send(String email, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        javaMailSender.send(message);
    }

    @Override
    public void sendTemporaryCode(String email, String token) {
        String body = messageSource.getMessage("email-template", new Object[] {token}, Locale.ROOT);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setText(body);
        message.setSubject(subject);

        javaMailSender.send(message);
    }
}
