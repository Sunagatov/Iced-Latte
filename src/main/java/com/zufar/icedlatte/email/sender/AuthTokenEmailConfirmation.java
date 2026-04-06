package com.zufar.icedlatte.email.sender;

import com.zufar.icedlatte.email.dto.EmailTokenDto;
import com.zufar.icedlatte.email.message.EmailConfirmMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "email.enabled", havingValue = "true")
public class AuthTokenEmailConfirmation extends AbstractEmailSender<EmailTokenDto> {

    @Value("${spring.mail.subject.confirmation}")
    private String subject;

    @Autowired
    public AuthTokenEmailConfirmation(JavaMailSender javaMailSender,
                                      SimpleMailMessage mailMessage,
                                      EmailConfirmMessage emailConfirmMessage) {
        super(javaMailSender, mailMessage, List.of(emailConfirmMessage));
    }

    public void sendTemporaryCode(String email,
                                  String message) {
        String buildMessage = getMessage(new EmailTokenDto(message));
        sendNotification(email, buildMessage, subject);
    }
}
