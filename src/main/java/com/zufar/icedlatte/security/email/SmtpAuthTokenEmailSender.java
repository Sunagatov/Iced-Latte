package com.zufar.icedlatte.security.email;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "email.enabled", havingValue = "true")
public class SmtpAuthTokenEmailSender implements AuthTokenEmailSender {

    private final JavaMailSender javaMailSender;
    private final SimpleMailMessage mailMessage;
    private final MessageSource messageSource;

    @Value("${spring.mail.subject.confirmation}")
    private String subject;

    @Override
    public void sendTemporaryCode(String email, String token) {
        String body = messageSource.getMessage("email-template", new Object[]{token}, Locale.ROOT);
        mailMessage.setTo(email);
        mailMessage.setText(body);
        mailMessage.setSubject(subject);
        javaMailSender.send(mailMessage);
    }
}
