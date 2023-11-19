package com.zufar.icedlatte.cart.exception.handler;

import com.zufar.icedlatte.cart.exception.InvalidItemProductQuantityException;
import com.zufar.icedlatte.cart.exception.InvalidShoppingSessionIdException;
import com.zufar.icedlatte.cart.exception.ShoppingSessionItemNotFoundException;
import com.zufar.icedlatte.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.common.exception.handler.ErrorDebugMessageCreator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartExceptionHandler Tests")
class CartExceptionHandlerTest {

    @Mock
    private ApiErrorResponseCreator apiErrorResponseCreator;

    @Mock
    private ErrorDebugMessageCreator errorDebugMessageCreator;

    @InjectMocks
    private CartExceptionHandler shoppingExceptionHandler;

    @Test
    @DisplayName("Should return ApiErrorResponse with NOT_FOUND status when ShoppingSessionNotFoundException is thrown")
    void shouldReturnApiErrorResponseWithNotFoundStatusWhenShoppingSessionNotFoundExceptionThrown() {
        UUID userId = UUID.randomUUID();
        LocalDateTime currentDateTime = LocalDateTime.now();
        ShoppingSessionNotFoundException exception = new ShoppingSessionNotFoundException(userId);
        ApiErrorResponse expectedResponse = new ApiErrorResponse(
                "The shopping session for the user with id = " + userId + " is not found.",
                HttpStatus.NOT_FOUND.value(),
                currentDateTime
        );

        when(apiErrorResponseCreator.buildResponse(exception, HttpStatus.NOT_FOUND)).thenReturn(expectedResponse);
        when(errorDebugMessageCreator.buildErrorDebugMessage(exception)).thenReturn("Error Debug Message");

        ApiErrorResponse actualResponse = shoppingExceptionHandler.handleShoppingSessionNotFoundException(exception);

        assertEquals(expectedResponse.httpStatusCode(), actualResponse.httpStatusCode());
        assertEquals(expectedResponse.message(), actualResponse.message());
        assertEquals(expectedResponse.timestamp(), actualResponse.timestamp());

        verify(apiErrorResponseCreator).buildResponse(exception, HttpStatus.NOT_FOUND);
        verify(errorDebugMessageCreator).buildErrorDebugMessage(exception);
    }

    @Test
    @DisplayName("Should return ApiErrorResponse with NOT_FOUND status when ShoppingSessionItemNotFoundException is thrown")
    void shouldReturnApiErrorResponseWithNotFoundStatusWhenShoppingSessionItemNotFoundExceptionThrown() {
        UUID shoppingSessionItemId = UUID.randomUUID();
        LocalDateTime currentDateTime = LocalDateTime.now();
        ShoppingSessionItemNotFoundException exception = new ShoppingSessionItemNotFoundException(shoppingSessionItemId);
        ApiErrorResponse expectedResponse = new ApiErrorResponse(
                "The shopping session item with id = " + shoppingSessionItemId + " is not found.",
                HttpStatus.NOT_FOUND.value(),
                currentDateTime
        );

        when(apiErrorResponseCreator.buildResponse(exception, HttpStatus.NOT_FOUND)).thenReturn(expectedResponse);
        when(errorDebugMessageCreator.buildErrorDebugMessage(exception)).thenReturn("Error Debug Message");

        ApiErrorResponse actualResponse = shoppingExceptionHandler.handleShoppingSessionItemNotFoundException(exception);

        assertEquals(expectedResponse.httpStatusCode(), actualResponse.httpStatusCode());
        assertEquals(expectedResponse.message(), actualResponse.message());
        assertEquals(expectedResponse.timestamp(), actualResponse.timestamp());

        verify(apiErrorResponseCreator).buildResponse(exception, HttpStatus.NOT_FOUND);
        verify(errorDebugMessageCreator).buildErrorDebugMessage(exception);
    }

    @Test
    @DisplayName("Should return ApiErrorResponse with BAD_REQUEST status when InvalidShoppingSessionIdException is thrown")
    void shouldReturnApiErrorResponseWithBadRequestStatusWhenInvalidShoppingSessionIdExceptionThrown() {
        UUID shoppingSessionId = UUID.randomUUID();
        LocalDateTime currentDateTime = LocalDateTime.now();
        InvalidShoppingSessionIdException exception = new InvalidShoppingSessionIdException(shoppingSessionId);
        ApiErrorResponse expectedResponse = new ApiErrorResponse(
                "The shopping session id = " + shoppingSessionId + " is invalid.",
                HttpStatus.BAD_REQUEST.value(),
                currentDateTime
        );

        when(apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST)).thenReturn(expectedResponse);
        when(errorDebugMessageCreator.buildErrorDebugMessage(exception)).thenReturn("Error Debug Message");

        ApiErrorResponse actualResponse = shoppingExceptionHandler.handleInvalidShoppingSessionIdException(exception);

        assertEquals(expectedResponse.httpStatusCode(), actualResponse.httpStatusCode());
        assertEquals(expectedResponse.message(), actualResponse.message());
        assertEquals(expectedResponse.timestamp(), actualResponse.timestamp());

        verify(apiErrorResponseCreator).buildResponse(exception, HttpStatus.BAD_REQUEST);
        verify(errorDebugMessageCreator).buildErrorDebugMessage(exception);
    }

    @Test
    @DisplayName("Should return ApiErrorResponse with BAD_REQUEST status when InvalidItemProductQuantityException is thrown")
    void shouldReturnApiErrorResponseWithBadRequestStatusWhenInvalidItemProductQuantityExceptionThrown() {
        Integer itemProductQuantity = 0;
        LocalDateTime currentDateTime = LocalDateTime.now();
        InvalidItemProductQuantityException exception = new InvalidItemProductQuantityException(itemProductQuantity);
        ApiErrorResponse expectedResponse = new ApiErrorResponse(
                "Invalid product quantity = " + itemProductQuantity + " or product quantity without changes.",
                HttpStatus.BAD_REQUEST.value(),
                currentDateTime
        );

        when(apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST)).thenReturn(expectedResponse);
        when(errorDebugMessageCreator.buildErrorDebugMessage(exception)).thenReturn("Error Debug Message");

        ApiErrorResponse actualResponse = shoppingExceptionHandler.handleInvalidItemProductQuantityException(exception);

        assertEquals(expectedResponse.httpStatusCode(), actualResponse.httpStatusCode());
        assertEquals(expectedResponse.message(), actualResponse.message());
        assertEquals(expectedResponse.timestamp(), actualResponse.timestamp());

        verify(apiErrorResponseCreator).buildResponse(exception, HttpStatus.BAD_REQUEST);
        verify(errorDebugMessageCreator).buildErrorDebugMessage(exception);
    }
}
