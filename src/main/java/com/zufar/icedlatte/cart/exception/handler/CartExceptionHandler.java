package com.zufar.icedlatte.cart.exception.handler;

import com.zufar.icedlatte.cart.exception.EmptyCartItemsException;
import com.zufar.icedlatte.cart.exception.InvalidItemProductQuantityException;
import com.zufar.icedlatte.cart.exception.InvalidShoppingCartIdException;
import com.zufar.icedlatte.cart.exception.ShoppingCartItemNotFoundException;
import com.zufar.icedlatte.cart.exception.ShoppingCartNotFoundException;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class CartExceptionHandler {

    private final ApiErrorResponseCreator apiErrorResponseCreator;

    @ExceptionHandler(EmptyCartItemsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @SuppressWarnings("unused")
    public ApiErrorResponse handleEmptyCartItemsException(final EmptyCartItemsException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.debug("exception.cart.empty_items: status=400");
        return apiErrorResponse;
    }

    @ExceptionHandler(InvalidItemProductQuantityException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleInvalidItemProductQuantityException(final InvalidItemProductQuantityException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.debug("exception.cart.quantity_invalid: exceptionClass={}, status=400", exception.getClass().getSimpleName());
        return apiErrorResponse;
    }

    @ExceptionHandler(InvalidShoppingCartIdException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleInvalidShoppingCartIdException(final InvalidShoppingCartIdException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.debug("exception.cart.id_invalid: exceptionClass={}, status=400", exception.getClass().getSimpleName());
        return apiErrorResponse;
    }

    @ExceptionHandler(ShoppingCartItemNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleShoppingCartItemNotFoundException(final ShoppingCartItemNotFoundException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.NOT_FOUND);
        log.debug("exception.cart.item_not_found: exceptionClass={}, status=404", exception.getClass().getSimpleName());
        return apiErrorResponse;
    }

    @ExceptionHandler(ShoppingCartNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleShoppingCartNotFoundException(final ShoppingCartNotFoundException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.NOT_FOUND);
        log.debug("exception.cart.not_found: exceptionClass={}, status=404", exception.getClass().getSimpleName());
        return apiErrorResponse;
    }
}
