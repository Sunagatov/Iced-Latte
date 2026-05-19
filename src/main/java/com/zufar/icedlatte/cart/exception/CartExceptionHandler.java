package com.zufar.icedlatte.cart.exception;

import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.common.exception.ProblemType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class CartExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(CartException.class)
    public ResponseEntity<ProblemDetail> handleCartException(final CartException ex) {
        record ErrorMapping(String logTag, String typeSlug, String title, HttpStatus status, String detail) {}

        final String errorMessage = ex.getMessage();
        var mapping = switch (ex) {
            case ShoppingCartNotFoundException _ ->
                    new ErrorMapping("exception.cart.not_found", ProblemType.CART_NOT_FOUND, "Cart not found", HttpStatus.NOT_FOUND, errorMessage);
            case ShoppingCartItemNotFoundException _ ->
                    new ErrorMapping("exception.cart.item_not_found", ProblemType.CART_ITEM_NOT_FOUND, "Cart item not found", HttpStatus.NOT_FOUND, errorMessage);
            case InvalidItemProductQuantityException _ ->
                    new ErrorMapping("exception.cart.invalid_quantity", ProblemType.CART_INVALID_QUANTITY, "Invalid quantity", HttpStatus.BAD_REQUEST, errorMessage);
        };

        HttpStatus status = mapping.status();
        log.debug("{}: status={}", mapping.logTag(), status.value());
        return ResponseEntity.status(status)
                .body(problemDetailFactory.build(mapping.typeSlug(), mapping.title(), status, mapping.detail()));
    }
}
