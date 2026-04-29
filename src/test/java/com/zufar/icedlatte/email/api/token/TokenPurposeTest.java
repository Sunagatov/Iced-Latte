package com.zufar.icedlatte.email.api.token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TokenPurpose")
class TokenPurposeTest {

    @Test
    @DisplayName("exposes the supported token purposes in stable order")
    void exposesSupportedTokenPurposesInStableOrder() {
        assertThat(TokenPurpose.values())
                .containsExactly(TokenPurpose.EMAIL_VERIFICATION, TokenPurpose.PASSWORD_RESET);
    }
}
