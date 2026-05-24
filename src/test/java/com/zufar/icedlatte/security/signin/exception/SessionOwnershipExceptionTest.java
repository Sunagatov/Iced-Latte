package com.zufar.icedlatte.security.signin.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SessionOwnershipException")
class SessionOwnershipExceptionTest {

    @Test
    @DisplayName("renders session id in ownership message")
    void rendersSessionIdInOwnershipMessage() {
        UUID sessionId = UUID.randomUUID();

        assertThat(new SessionOwnershipException(sessionId))
                .hasMessage("Session does not belong to the requesting user: " + sessionId);
    }
}
