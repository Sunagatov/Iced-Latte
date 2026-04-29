package com.zufar.icedlatte.email.sender;

import com.zufar.icedlatte.email.exception.MessageBuilderNotFoundException;
import com.zufar.icedlatte.email.message.MessageBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AbstractEmailSender")
class AbstractEmailSenderTest {

    private final JavaMailSender javaMailSender = mock(JavaMailSender.class);
    private final SimpleMailMessage mailMessage = new SimpleMailMessage();
    @SuppressWarnings("unchecked")
    private final MessageBuilder<Object> messageBuilder = mock(MessageBuilder.class);

    @Test
    @DisplayName("sendNotification writes message fields and sends mail")
    void sendNotificationWritesFieldsAndSendsMail() {
        TestEmailSender sender = new TestEmailSender(javaMailSender, mailMessage, List.of(messageBuilder));

        sender.sendNotification("user@example.com", "Hello there", "Subject");

        assertThat(mailMessage.getTo()).containsExactly("user@example.com");
        assertThat(mailMessage.getText()).isEqualTo("Hello there");
        assertThat(mailMessage.getSubject()).isEqualTo("Subject");
        verify(javaMailSender).send(mailMessage);
    }

    @Test
    @DisplayName("sendNotification rethrows mail delivery failures")
    void sendNotificationRethrowsFailures() {
        TestEmailSender sender = new TestEmailSender(javaMailSender, mailMessage, List.of(messageBuilder));
        MailSendException exception = new MailSendException("boom");
        doThrow(exception).when(javaMailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> sender.sendNotification("user@example.com", "Hello there", "Subject"))
                .isSameAs(exception);
    }

    @Test
    @DisplayName("getMessage uses the first supporting builder")
    void getMessageUsesSupportingBuilder() {
        Object event = new Object();
        when(messageBuilder.supports(event.getClass())).thenReturn(true);
        when(messageBuilder.buildMessage(eq(event), eq(Locale.ROOT))).thenReturn("Built message");
        TestEmailSender sender = new TestEmailSender(javaMailSender, mailMessage, List.of(messageBuilder));

        String message = sender.getMessageFor(event);

        assertThat(message).isEqualTo("Built message");
        verify(messageBuilder).buildMessage(event, Locale.ROOT);
    }

    @Test
    @DisplayName("getMessage fails when no builder supports the event")
    void getMessageFailsWhenBuilderMissing() {
        Object event = new Object();
        when(messageBuilder.supports(event.getClass())).thenReturn(false);
        TestEmailSender sender = new TestEmailSender(javaMailSender, mailMessage, List.of(messageBuilder));

        assertThatThrownBy(() -> sender.getMessageFor(event))
                .isInstanceOf(MessageBuilderNotFoundException.class)
                .hasMessageContaining(event.getClass().getName());
    }

    private static final class TestEmailSender extends AbstractEmailSender<Object> {

        private TestEmailSender(JavaMailSender javaMailSender,
                                SimpleMailMessage mailMessage,
                                List<MessageBuilder<Object>> messageBuilders) {
            super(javaMailSender, mailMessage, messageBuilders);
        }

        private String getMessageFor(Object event) {
            return getMessage(event);
        }
    }
}
