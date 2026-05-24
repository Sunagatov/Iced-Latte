package com.zufar.icedlatte.security.email.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.security.email.sender.AuthTokenEmailSender;

@DisplayName("EmailDisabledConfig")
class EmailDisabledConfigTest {

    private final EmailDisabledConfig config = new EmailDisabledConfig();

    @Test
    @DisplayName("returns auth sender that skips delivery")
    void returnsNoOpAuthTokenSender() {
        AuthTokenEmailSender confirmation = config.noOpAuthTokenEmailSender();

        confirmation.sendTemporaryCode("user@example.com", "123456");

        assertThat(confirmation).isNotNull();
    }
}
