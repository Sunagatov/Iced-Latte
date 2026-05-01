package com.zufar.icedlatte.email.config;

import com.zufar.icedlatte.email.sender.AuthTokenEmailSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmailConfig unit tests")
class EmailConfigTest {

    @Test
    @DisplayName("builds a JavaMailSender with SMTP defaults")
    void buildsJavaMailSenderWithSmtpDefaults() {
        EmailConfig config = new EmailConfig();
        ReflectionTestUtils.setField(config, "host", "smtp.example.com");
        ReflectionTestUtils.setField(config, "port", 2525);
        ReflectionTestUtils.setField(config, "username", "mailer");
        ReflectionTestUtils.setField(config, "password", "secret");

        JavaMailSender sender = config.getJavaMailSender();

        assertThat(sender).isInstanceOf(JavaMailSenderImpl.class);
        JavaMailSenderImpl mailSender = (JavaMailSenderImpl) sender;
        assertThat(mailSender.getHost()).isEqualTo("smtp.example.com");
        assertThat(mailSender.getPort()).isEqualTo(2525);
        assertThat(mailSender.getUsername()).isEqualTo("mailer");
        assertThat(mailSender.getPassword()).isEqualTo("secret");
        assertThat(mailSender.getJavaMailProperties())
                .containsEntry("mail.transport.protocol", "smtp")
                .containsEntry("mail.smtp.auth", "true")
                .containsEntry("mail.smtp.starttls.enable", "true");
    }

    @Test
    @DisplayName("creates reusable mail and message-source beans")
    void createsMailAndMessageSourceBeans() {
        EmailConfig config = new EmailConfig();

        SimpleMailMessage mailMessage = config.simpleMailMessage();
        MessageSource messageSource = config.messageSource();

        assertThat(mailMessage).isNotNull();
        assertThat(messageSource).isInstanceOf(ReloadableResourceBundleMessageSource.class);
        ReloadableResourceBundleMessageSource bundle = (ReloadableResourceBundleMessageSource) messageSource;
        assertThat(bundle.getBasenameSet()).containsExactly("classpath:messages/messages");
        assertThat(bundle.getMessage("email-template", new Object[]{"123456"}, Locale.ENGLISH))
                .contains("123456")
                .contains("5 minutes");
    }

    @Test
    @DisplayName("disabled config returns a no-op auth token sender")
    void disabledConfigReturnsNoOpSender() {
        EmailDisabledConfig config = new EmailDisabledConfig();
        AuthTokenEmailSender confirmation = config.noOpAuthTokenEmailSender();
        assertThat(confirmation).isNotNull();

        confirmation.sendTemporaryCode("user@example.com", "123456");
    }
}
