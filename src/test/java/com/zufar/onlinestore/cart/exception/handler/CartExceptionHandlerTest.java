package com.zufar.onlinestore.cart.exception.handler;

import com.zufar.onlinestore.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.onlinestore.common.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CartExceptionHandlerTest {

    private CartExceptionHandler cartExceptionHandler;

    @BeforeEach
    void setUp() { cartExceptionHandler = new CartExceptionHandler(); }
    @Test
    void handlerShoppingSessionNotFoundException_ShouldReturnApiResponseWithNotFoundStatus() {
        UUID userId = UUID.randomUUID();
        ShoppingSessionNotFoundException exception = new ShoppingSessionNotFoundException(userId);

        ApiResponse<Void> apiResponse = cartExceptionHandler.handleShoppingSessionNotFoundException(exception);

        assertEquals(HttpStatus.NOT_FOUND.value(), apiResponse.httpStatusCode());
        assertEquals("The shopping session for the user with id = " + userId + " is not found.", apiResponse.messages().get(0));
        assertTrue(apiResponse.description().contains("Operation was failed in method: handlerShoppingSessionNotFoundException_ShouldReturnApiResponseWithNotFoundStatus that belongs to the class: com.zufar.onlinestore.cart.exception.handler.CartExceptionHandlerTest. Problematic code line: "));
    }
}
