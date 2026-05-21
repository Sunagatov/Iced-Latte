package com.zufar.icedlatte.security.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserAccountLockedException")
class UserAccountLockedExceptionTest {

    @Test
    @DisplayName("renders lockout duration in message")
    void rendersLockoutDurationInMessage() {
        assertThat(new UserAccountLockedException(15))
                .hasMessage("Account temporarily locked due to too many failed login attempts. Try again in 15 minutes or reset your password.");
    }
}
