package com.zufar.icedlatte.email.message;

import com.zufar.icedlatte.email.dto.EmailTokenDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EmailConfirmMessage")
class EmailConfirmMessageTest {

    private final MessageSource messageSource = mock(MessageSource.class);
    private final EmailConfirmMessage messageBuilder = new EmailConfirmMessage(messageSource);

    @Test
    @DisplayName("builds localized confirmation message from message source")
    void buildsLocalizedMessage() {
        EmailTokenDto dto = new EmailTokenDto("123456");
        when(messageSource.getMessage("email-template", new Object[]{"123456"}, Locale.UK))
                .thenReturn("Your code is 123456");

        String result = messageBuilder.buildMessage(dto, Locale.UK);

        assertThat(result).isEqualTo("Your code is 123456");
        verify(messageSource).getMessage("email-template", new Object[]{"123456"}, Locale.UK);
    }

    @Test
    @DisplayName("supports email token dto classes only")
    void supportsEmailTokenDto() {
        assertThat(messageBuilder.supports(EmailTokenDto.class)).isTrue();
        assertThat(messageBuilder.supports(Object.class)).isFalse();
    }
}
