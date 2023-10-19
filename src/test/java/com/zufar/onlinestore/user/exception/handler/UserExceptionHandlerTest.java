package com.zufar.onlinestore.user.exception.handler;

import com.zufar.onlinestore.common.response.ApiResponse;
import com.zufar.onlinestore.user.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserExceptionHandlerTest {

    private UserExceptionHandler userExceptionHandler;

    @BeforeEach
    void setUp() {
        userExceptionHandler = new UserExceptionHandler();
    }

    @Test
    void handleUserNotFoundException_ShouldReturnApiResponseWithNotFoundStatus() {
        UUID userId = UUID.randomUUID();
        UserNotFoundException exception = new UserNotFoundException(userId);

        ApiResponse<Void> apiResponse = userExceptionHandler.handleUserNotFoundException(exception);

        assertEquals(HttpStatus.NOT_FOUND.value(), apiResponse.httpStatusCode());
        assertEquals("User with id = " + userId + " is not found.", apiResponse.messages().get(0));
        assertEquals("Operation was failed in method: handleUserNotFoundException_ShouldReturnApiResponseWithNotFoundStatus that belongs to the class: com.zufar.onlinestore.user.exception.handler.UserExceptionHandlerTest. Problematic code line: 28", apiResponse.description());
    }
}