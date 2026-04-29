package com.zufar.icedlatte.email.sender;

import com.zufar.icedlatte.email.message.EmailConfirmMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AuthTokenEmailConfirmation")
class AuthTokenEmailConfirmationTest {

    private final JavaMailSender javaMailSender = mock(JavaMailSender.class);
    private final MessageSource messageSource = mock(MessageSource.class);
    private final SimpleMailMessage mailMessage = new SimpleMailMessage();

    @Test
    @DisplayName("builds confirmation email body and sends it with configured subject")
    void sendsTemporaryCodeWithRenderedBodyAndSubject() {
        when(messageSource.getMessage("email-template", new Object[]{"654321"}, Locale.ROOT))
                .thenReturn("Use code 654321");
        EmailConfirmMessage emailConfirmMessage = new EmailConfirmMessage(messageSource);
        AuthTokenEmailConfirmation confirmation =
                new AuthTokenEmailConfirmation(javaMailSender, mailMessage, emailConfirmMessage);
        ReflectionTestUtils.setField(confirmation, "subject", "Confirm your email");

        confirmation.sendTemporaryCode("user@example.com", "654321");

        assertThat(mailMessage.getTo()).containsExactly("user@example.com");
        assertThat(mailMessage.getText()).isEqualTo("Use code 654321");
        assertThat(mailMessage.getSubject()).isEqualTo("Confirm your email");
        verify(javaMailSender).send(mailMessage);
    }
}
