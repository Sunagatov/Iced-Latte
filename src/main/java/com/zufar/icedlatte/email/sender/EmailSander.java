package com.zufar.icedlatte.email.sender;

import com.zufar.icedlatte.email.message.MessageBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public abstract class EmailSander<T> {

    private final JavaMailSender javaMailSender;
    private final SimpleMailMessage mailMessage;
    private final List<MessageBuilder<T>> messageBuilders;

    protected void sendNotification(String email, String message, String subject) {
        mailMessage.setTo(email);
        mailMessage.setText(message);
        mailMessage.setSubject(subject);
        javaMailSender.send(mailMessage);
    }

    protected String getMessage(Class<?> clazz, T event) {
        return messageBuilders.stream()
                .filter(builder -> builder.supports(clazz))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No message builder found"))
                .buildMessage(event, Locale.ENGLISH);
    }
}
