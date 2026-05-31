package com.zufar.icedlatte.cart.exception.handler;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.zufar.icedlatte.cart.exception.CartException;
import com.zufar.icedlatte.cart.exception.CartProductNotFoundException;
import com.zufar.icedlatte.cart.exception.CartProductSnapshotMissingException;
import com.zufar.icedlatte.cart.exception.InvalidCartItemRequestException;
import com.zufar.icedlatte.cart.exception.InvalidItemProductQuantityException;
import com.zufar.icedlatte.cart.exception.ShoppingCartItemNotFoundException;
import com.zufar.icedlatte.cart.exception.ShoppingCartNotFoundException;
import com.zufar.icedlatte.common.exception.ProblemType;
import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class CartExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(CartException.class)
    public ResponseEntity<ProblemDetail> handleCartException(final CartException ex) {
        final String errorMessage = ex.getMessage();
        var mapping =
                switch (ex) {
                    case ShoppingCartNotFoundException _ ->
                        new ErrorMapping(
                                "exception.cart.not_found",
                                ProblemType.CART_NOT_FOUND,
                                "Cart not found",
                                HttpStatus.NOT_FOUND,
                                errorMessage);
                    case ShoppingCartItemNotFoundException _ ->
                        new ErrorMapping(
                                "exception.cart.item_not_found",
                                ProblemType.CART_ITEM_NOT_FOUND,
                                "Cart item not found",
                                HttpStatus.NOT_FOUND,
                                errorMessage);
                    case InvalidItemProductQuantityException _ ->
                        new ErrorMapping(
                                "exception.cart.invalid_quantity",
                                ProblemType.CART_INVALID_QUANTITY,
                                "Invalid quantity",
                                HttpStatus.BAD_REQUEST,
                                errorMessage);
                    case InvalidCartItemRequestException _ ->
                        new ErrorMapping(
                                "exception.cart.invalid_item_request",
                                ProblemType.CART_INVALID_ITEM_REQUEST,
                                "Invalid cart item request",
                                HttpStatus.BAD_REQUEST,
                                errorMessage);
                    case CartProductNotFoundException _ ->
                        new ErrorMapping(
                                "exception.cart.product_not_found",
                                ProblemType.CART_PRODUCT_NOT_FOUND,
                                "Product not found",
                                HttpStatus.NOT_FOUND,
                                errorMessage);
                    case CartProductSnapshotMissingException _ ->
                        new ErrorMapping(
                                "exception.cart.product_snapshot_missing",
                                ProblemType.CART_PRODUCT_SNAPSHOT_MISSING,
                                "Cart product data unavailable",
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                errorMessage);
                };

        HttpStatus status = mapping.status();
        log.debug("{}: status={}", mapping.logTag(), status.value());
        return ResponseEntity.status(status)
                .body(problemDetailFactory.build(mapping.typeSlug(), mapping.title(), status, mapping.detail()));
    }

    private record ErrorMapping(String logTag, String typeSlug, String title, HttpStatus status, String detail) {}
}
