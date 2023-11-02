package com.zufar.onlinestore.cart.exception.handler;

import com.zufar.onlinestore.cart.exception.InvalidItemProductQuantityException;
import com.zufar.onlinestore.cart.exception.InvalidShoppingSessionIdException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionItemNotFoundException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.onlinestore.common.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Should return API response with not found status for shopping session")
    void shouldReturnApiResponseWithShoppingSessionNotFoundStatus() {
        UUID userId = UUID.randomUUID();
        ShoppingSessionNotFoundException exception = new ShoppingSessionNotFoundException(userId);

        ApiResponse<Void> apiResponse = cartExceptionHandler.handleShoppingSessionNotFoundException(exception);

        assertEquals(HttpStatus.NOT_FOUND.value(), apiResponse.httpStatusCode());
        assertEquals("The shopping session for the user with id = " + userId + " is not found.", apiResponse.messages().get(0));
        assertTrue(apiResponse.description().contains("Operation was failed in method: shouldReturnApiResponseWithShoppingSessionNotFoundStatus that belongs to the class: com.zufar.onlinestore.cart.exception.handler.CartExceptionHandlerTest. Problematic code line: "));
    }

    @Test
    @DisplayName("Should return API response with not found status for shopping session item")
    void shouldReturnApiResponseWithShoppingSessionItemNotFoundStatus() {
        UUID userId = UUID.randomUUID();
        ShoppingSessionItemNotFoundException exception = new ShoppingSessionItemNotFoundException(userId);

        ApiResponse<Void> apiResponse = cartExceptionHandler.handleShoppingSessionItemNotFoundException(exception);

        assertEquals(HttpStatus.NOT_FOUND.value(), apiResponse.httpStatusCode());
        assertEquals("The shopping session item with shoppingSessionItemId = " + userId + " is not found.", apiResponse.messages().get(0));
        assertTrue(apiResponse.description().contains("Operation was failed in method: shouldReturnApiResponseWithShoppingSessionItemNotFoundStatus that belongs to the class: com.zufar.onlinestore.cart.exception.handler.CartExceptionHandlerTest. Problematic code line: "));
    }

    @Test
    @DisplayName("Should return API response with invalid shopping session id status")
    void shouldReturnApiResponseWithInvalidShoppingSessionIdStatus() {
        UUID userId = UUID.randomUUID();
        InvalidShoppingSessionIdException exception = new InvalidShoppingSessionIdException(userId);

        ApiResponse<Void> apiResponse = cartExceptionHandler.handleInvalidShoppingSessionIdException(exception);

        assertEquals(HttpStatus.BAD_REQUEST.value(), apiResponse.httpStatusCode());
        assertEquals("The shopping session id = " + userId + " is invalid in UpdateProductsQuantityInShoppingSessionItemRequest.", apiResponse.messages().get(0));
        assertTrue(apiResponse.description().contains("Operation was failed in method: shouldReturnApiResponseWithInvalidShoppingSessionIdStatus that belongs to the class: com.zufar.onlinestore.cart.exception.handler.CartExceptionHandlerTest. Problematic code line: "));
    }

    @Test
    @DisplayName("Should return API response with invalid item product quantity status")
    void shouldReturnApiResponseWithInvalidItemProductQuantityStatus() {
        int itemProductQuantity = 0;
        InvalidItemProductQuantityException exception = new InvalidItemProductQuantityException(itemProductQuantity);

        ApiResponse<Void> apiResponse = cartExceptionHandler.handleInvalidItemProductQuantityException(exception);

        assertEquals(HttpStatus.BAD_REQUEST.value(), apiResponse.httpStatusCode());
        assertEquals("Invalid product quantity = " + itemProductQuantity + " or product quantity without changes", apiResponse.messages().get(0));
        assertTrue(apiResponse.description().contains("Operation was failed in method: shouldReturnApiResponseWithInvalidItemProductQuantityStatus that belongs to the class: com.zufar.onlinestore.cart.exception.handler.CartExceptionHandlerTest. Problematic code line: "));
    }
}
