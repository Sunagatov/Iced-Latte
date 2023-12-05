package com.zufar.icedlatte.email.message;

import com.zufar.icedlatte.email.dto.EmailConfirmDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class EmailConfirmMessage implements MessageBuilder<EmailConfirmDto> {

    private final MessageSource messageSource;
    @Override
    public String buildMessage(EmailConfirmDto event, Locale locale) {
        return messageSource.getMessage("email-template", new Object[]{event.code()}, locale);
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz == EmailConfirmMessage.class;
    }
}
