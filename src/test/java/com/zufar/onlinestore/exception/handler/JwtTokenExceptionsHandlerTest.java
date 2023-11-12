package com.zufar.onlinestore.exception.handler;

import com.zufar.onlinestore.security.exception.JwtTokenException;
import com.zufar.onlinestore.security.exception.handler.JwtTokenExceptionsHandler;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JwtTokenExceptionsHandlerTest{


    @Test
    public void testHandleJwtTokenException() {
        JwtTokenException exception = new JwtTokenException("Test exception");
        JwtTokenExceptionsHandler handler = new JwtTokenExceptionsHandler();

        ResponseEntity<Map<String, String>> response = handler.handleJwtTokenException(exception);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Test exception", response.getBody().get("JwtToken Error message"));
        assertNotNull(response.getBody().get("JwtToken Cause Error message"));
    }
}
