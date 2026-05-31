package com.zufar.icedlatte.favorite.exception.handler;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.favorite.exception.FavoriteException;
import com.zufar.icedlatte.favorite.exception.FavoriteProductNotFoundException;
import com.zufar.icedlatte.favorite.exception.InvalidFavoriteRequestException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class FavoriteExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(FavoriteException.class)
    public ResponseEntity<ProblemDetail> handleFavoriteException(final FavoriteException ex) {
        final String errorMessage = ex.getMessage();
        var mapping =
                switch (ex) {
                    case FavoriteProductNotFoundException _ ->
                        new ErrorMapping(
                                "exception.favorite.product_not_found",
                                "favorite-product-not-found",
                                "Product not found",
                                HttpStatus.NOT_FOUND,
                                errorMessage);
                    case InvalidFavoriteRequestException _ ->
                        new ErrorMapping(
                                "exception.favorite.invalid_request",
                                "favorite-invalid-request",
                                "Invalid favorite request",
                                HttpStatus.BAD_REQUEST,
                                errorMessage);
                };

        HttpStatus status = mapping.status();
        log.debug("{}: status={}", mapping.logTag(), status.value());
        return ResponseEntity.status(status)
                .body(problemDetailFactory.build(mapping.typeSlug(), mapping.title(), status, mapping.detail()));
    }

    private record ErrorMapping(String logTag, String typeSlug, String title, HttpStatus status, String detail) {}
}
