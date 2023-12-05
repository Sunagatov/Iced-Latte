package com.zufar.icedlatte.email.sender;

import com.zufar.icedlatte.email.dto.EmailConfirmDto;
import com.zufar.icedlatte.email.message.EmailConfirmMessage;
import com.zufar.icedlatte.email.message.MessageBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmailConfirmation extends EmailSander {

    @Value("${spring.mail.subject.confirmation}")
    private String subject;

    @Autowired
    public EmailConfirmation(JavaMailSender javaMailSender,
                             SimpleMailMessage mailMessage,
                             List<MessageBuilder<EmailConfirmDto>> messageBuilders) {
        super(javaMailSender, mailMessage, messageBuilders);
    }

    public void sendTemporaryCode(String email, String message) {
        message = getMessage(EmailConfirmMessage.class, new EmailConfirmDto(message));
        sendNotification(email, message, subject);
    }
}
