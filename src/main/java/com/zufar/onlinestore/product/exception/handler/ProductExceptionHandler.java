package com.zufar.onlinestore.product.exception.handler;

import com.zufar.onlinestore.common.exception.handler.GlobalExceptionHandler;
import com.zufar.onlinestore.common.response.ApiResponse;
import com.zufar.onlinestore.product.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RequiredArgsConstructor
@RestControllerAdvice
@Slf4j
public class ProductExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleProductNotFoundException(final ProductNotFoundException exception) {
        ApiResponse<Void> apiResponse = buildResponse(exception, HttpStatus.NOT_FOUND);
        log.error("Handle product not found exception: failed: messages: {}, description: {}.",
                apiResponse.messages(), apiResponse.description());

        return apiResponse;
    }
}
