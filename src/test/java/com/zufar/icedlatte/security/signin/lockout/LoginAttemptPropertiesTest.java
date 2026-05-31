package com.zufar.icedlatte.security.signin.lockout;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LoginAttemptProperties unit tests")
class LoginAttemptPropertiesTest {

    @Test
    @DisplayName("rejects missing max attempts")
    void rejectsMissingMaxAttempts() {
        assertThatThrownBy(() -> new LoginAttemptProperties(null, 15))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("login-attempts.max-attempts must not be null");
    }

    @Test
    @DisplayName("rejects missing lockout duration")
    void rejectsMissingLockoutDuration() {
        assertThatThrownBy(() -> new LoginAttemptProperties(5, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("login-attempts.lockout-duration-minutes must not be null");
    }

    @Test
    @DisplayName("rejects non-positive values")
    void rejectsNonPositiveValues() {
        assertThatThrownBy(() -> new LoginAttemptProperties(0, 15))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("login-attempts.max-attempts must be at least 1");

        assertThatThrownBy(() -> new LoginAttemptProperties(5, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("login-attempts.lockout-duration-minutes must be at least 1");
    }
}
