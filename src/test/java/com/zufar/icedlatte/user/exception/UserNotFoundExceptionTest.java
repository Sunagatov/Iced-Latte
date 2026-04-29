package com.zufar.icedlatte.user.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserNotFoundException")
class UserNotFoundExceptionTest {

    @Test
    @DisplayName("uuid constructor renders missing user id")
    void uuidConstructorRendersMissingUserId() {
        UUID userId = UUID.randomUUID();

        UserNotFoundException exception = new UserNotFoundException(userId);

        assertThat(exception).hasMessage("User with id = " + userId + " is not found.");
    }

    @Test
    @DisplayName("email constructor renders missing user email")
    void emailConstructorRendersMissingUserEmail() {
        UserNotFoundException exception = new UserNotFoundException("user@example.com");

        assertThat(exception).hasMessage("User with email = user@example.com is not found.");
    }
}
