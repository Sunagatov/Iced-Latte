package com.zufar.icedlatte.security.email.config;

import com.zufar.icedlatte.security.email.sender.AuthTokenEmailSender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmailConfig unit tests")
class EmailConfigTest {

    @Test
    @DisplayName("creates message-source bean")
    void createsMessageSourceBean() {
        EmailConfig config = new EmailConfig();

        MessageSource messageSource = config.messageSource();

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
