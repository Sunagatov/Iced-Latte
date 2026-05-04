package com.zufar.icedlatte.email.sender;

import com.zufar.icedlatte.common.exception.InternalServerErrorException;
import com.zufar.icedlatte.email.message.MessageBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public abstract class AbstractEmailSender<T> {

    private final JavaMailSender javaMailSender;
    private final SimpleMailMessage mailMessage;
    private final List<MessageBuilder<T>> messageBuilders;

    @PostConstruct
    void validateBuilders() {
        log.debug("email.builders.registered: senderClass={}, count={}",
                getClass().getSimpleName(), messageBuilders.size());
    }

    public void sendNotification(String email,
                                 String message,
                                 String subject) {
        try {
            mailMessage.setTo(email);
            mailMessage.setText(message);
            mailMessage.setSubject(subject);
            javaMailSender.send(mailMessage);
            log.info("email.sent: senderClass={}", getClass().getSimpleName());
        } catch (Exception e) {
            log.error("email.send.failed: senderClass={}, exceptionClass={}",
                    getClass().getSimpleName(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    protected String getMessage(T event) {
        return messageBuilders.stream()
                .filter(builder -> builder.supports(event.getClass()))
                .findFirst()
                .orElseThrow(() -> new InternalServerErrorException(
                        "No message builder found for " + event.getClass().getName()))
                .buildMessage(event, Locale.ROOT);
    }
}
