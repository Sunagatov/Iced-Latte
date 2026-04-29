package com.zufar.icedlatte.email.config;

import com.zufar.icedlatte.email.message.EmailConfirmMessage;
import com.zufar.icedlatte.email.sender.AuthTokenEmailConfirmation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("EmailDisabledConfig")
class EmailDisabledConfigTest {

    private final EmailDisabledConfig config = new EmailDisabledConfig();

    @Test
    @DisplayName("creates no-op mail beans")
    void createsNoOpMailBeans() {
        JavaMailSender mailSender = config.noOpMailSender();
        SimpleMailMessage mailMessage = config.noOpMailMessage();

        assertThat(mailSender).isInstanceOf(JavaMailSenderImpl.class);
        assertThat(mailMessage).isNotNull();
    }

    @Test
    @DisplayName("returns auth sender that skips mail delivery")
    void returnsNoOpAuthTokenSender() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        EmailConfirmMessage emailConfirmMessage = new EmailConfirmMessage(mock(MessageSource.class));

        AuthTokenEmailConfirmation confirmation =
                config.noOpAuthTokenEmailConfirmation(mailSender, mailMessage, emailConfirmMessage);

        confirmation.sendTemporaryCode("user@example.com", "123456");

        verifyNoInteractions(mailSender);
        assertThat(mailMessage.getTo()).isNull();
        assertThat(mailMessage.getText()).isNull();
        assertThat(mailMessage.getSubject()).isNull();
    }
}
