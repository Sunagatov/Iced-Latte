package com.zufar.icedlatte.security.exception.handler;

import com.zufar.icedlatte.security.exception.JwtTokenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenExceptionsHandler Tests")
class JwtTokenExceptionsHandlerTest {

    private final JwtTokenExceptionsHandler jwtTokenExceptionsHandler = new JwtTokenExceptionsHandler();

    @Test
    @DisplayName("Should return UNAUTHORIZED ResponseEntity with error details when JwtTokenException is thrown")
    void shouldReturnUnauthorizedResponseEntityWhenJwtTokenExceptionThrown() {
        Throwable cause = new RuntimeException("Cause error message");
        JwtTokenException exception = new JwtTokenException("Jwt token error message", cause);

        ResponseEntity<Map<String, String>> response = jwtTokenExceptionsHandler.handleJwtTokenException(exception);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        Map<String, String> errors = response.getBody();
        assertNotNull(errors);
        assertEquals("Jwt token error message", errors.get("JwtToken Error message"));
        assertEquals("Cause error message", errors.get("JwtToken Cause Error message"));
    }
}
