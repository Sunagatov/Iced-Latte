package com.zufar.icedlatte.security.signin.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SessionNotFoundException")
class SessionNotFoundExceptionTest {

    @Test
    @DisplayName("renders missing session id in message")
    void rendersMissingSessionIdInMessage() {
        UUID sessionId = UUID.randomUUID();

        assertThat(new SessionNotFoundException(sessionId)).hasMessage("Session not found: " + sessionId);
    }
}
