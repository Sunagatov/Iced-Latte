package com.zufar.icedlatte.product.exception.handler;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.product.exception.GetProductsBadRequestException;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Order(0)
public class ProductExceptionHandler {

    private final ApiErrorResponseCreator apiErrorResponseCreator;

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleProductNotFoundException(final ProductNotFoundException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.NOT_FOUND);
        log.debug("exception.product.not_found: exceptionClass={}, status=404", exception.getClass().getSimpleName());
        return apiErrorResponse;
    }

    @ExceptionHandler(GetProductsBadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleGetProductsBadRequestException(final GetProductsBadRequestException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);

        log.debug("exception.product.invalid_property: exceptionClass={}, status=400", exception.getClass().getSimpleName());

        return apiErrorResponse;
    }
}
