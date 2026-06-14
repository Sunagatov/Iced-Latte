package com.zufar.icedlatte.product.exception.handler;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.zufar.icedlatte.common.exception.ProblemType;
import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ProductExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleProductNotFoundException(final ProductNotFoundException ex) {
        log.debug("exception.product.not_found: status=404");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(problemDetailFactory.build(
                        ProblemType.PRODUCT_NOT_FOUND, "Product not found", HttpStatus.NOT_FOUND, ex.getMessage()));
    }
}
