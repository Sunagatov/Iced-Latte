package com.zufar.icedlatte.security.email;

import com.zufar.icedlatte.security.email.sender.SmtpAuthTokenEmailSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.MessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("SmtpAuthTokenEmailSender")
class SmtpAuthTokenEmailSenderTest {

    private final JavaMailSender javaMailSender = mock(JavaMailSender.class);
    private final MessageSource messageSource = mock(MessageSource.class);

    @Test
    @DisplayName("builds confirmation email body and sends it with configured subject")
    void sendsTemporaryCodeWithRenderedBodyAndSubject() {
        when(messageSource.getMessage("email-template", new Object[]{"654321"}, Locale.ROOT))
                .thenReturn("Use code 654321");
        SmtpAuthTokenEmailSender sender =
                new SmtpAuthTokenEmailSender(javaMailSender, messageSource);
        ReflectionTestUtils.setField(sender, "subject", "Confirm your email");

        sender.sendTemporaryCode("user@example.com", "654321");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("user@example.com");
        assertThat(sent.getText()).isEqualTo("Use code 654321");
        assertThat(sent.getSubject()).isEqualTo("Confirm your email");
    }
}
