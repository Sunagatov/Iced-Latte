package com.zufar.icedlatte.email.config;

import com.zufar.icedlatte.email.dto.EmailTokenDto;
import com.zufar.icedlatte.email.sender.AuthTokenEmailConfirmation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "email.enabled", havingValue = "false", matchIfMissing = true)
public class EmailDisabledConfig {

    @Bean
    public JavaMailSender noOpMailSender() {
        return new JavaMailSenderImpl();
    }

    @Bean
    public SimpleMailMessage noOpMailMessage() {
        return new SimpleMailMessage();
    }

    @Bean
    public AuthTokenEmailConfirmation noOpAuthTokenEmailConfirmation(
            JavaMailSender mailSender, SimpleMailMessage mailMessage) {
        return new AuthTokenEmailConfirmation(mailSender, mailMessage, java.util.List.of()) {
            @Override
            public void sendTemporaryCode(String email, String message) {
                log.debug("email.send.skipped: email.enabled=false");
            }
        };
    }
}
