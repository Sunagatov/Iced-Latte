package com.zufar.onlinestore.cart.exception.handler;

import com.zufar.onlinestore.cart.exception.InvalidItemProductQuantityException;
import com.zufar.onlinestore.cart.exception.InvalidShoppingSessionIdException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionItemNotFoundException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.onlinestore.common.exception.handler.GlobalExceptionHandler;
import com.zufar.onlinestore.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RequiredArgsConstructor
@RestControllerAdvice
@Slf4j
public class CartExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(InvalidItemProductQuantityException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleInvalidItemProductQuantityException(final InvalidItemProductQuantityException exception) {
        ApiResponse<Void> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle invalid item's product quantity exception: failed: messages: {}, description: {}.",
                apiResponse.messages(), apiResponse.description());

        return apiResponse;
    }

    @ExceptionHandler(InvalidShoppingSessionIdException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleInvalidShoppingSessionIdException(final InvalidShoppingSessionIdException exception) {
        ApiResponse<Void> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle invalid shopping sessionId exception: failed: messages: {}, description: {}.",
                apiResponse.messages(), apiResponse.description());

        return apiResponse;
    }

    @ExceptionHandler(ShoppingSessionItemNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleShoppingSessionItemNotFoundException(final ShoppingSessionItemNotFoundException exception) {
        ApiResponse<Void> apiResponse = buildResponse(exception, HttpStatus.NOT_FOUND);
        log.error("Handle shopping session item not found exception: failed: messages: {}, description: {}.",
                apiResponse.messages(), apiResponse.description());

        return apiResponse;
    }

    @ExceptionHandler(ShoppingSessionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleShoppingSessionNotFoundException(final ShoppingSessionNotFoundException exception) {
        ApiResponse<Void> apiResponse = buildResponse(exception, HttpStatus.NOT_FOUND);
        log.error("Handle shopping session not found exception: failed: messages: {}, description: {}.",
                apiResponse.messages(), apiResponse.description());

        return apiResponse;
    }
}
