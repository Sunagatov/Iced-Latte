package com.zufar.icedlatte.order.exception.handler;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.exception.InvalidOrderStateTransitionException;
import com.zufar.icedlatte.order.exception.OrderAccessDeniedException;
import com.zufar.icedlatte.order.exception.OrderCancellationWindowExpiredException;
import com.zufar.icedlatte.order.exception.OrderNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Arrays;

@Slf4j
@RestControllerAdvice(basePackages = {"com.zufar.icedlatte.order.endpoint"})
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class OrderExceptionHandler {

    private final ApiErrorResponseCreator apiErrorResponseCreator;

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleTypeMismatch(final MethodArgumentTypeMismatchException ignored) {
        String message = "Incorrect status value. Supported status: " + Arrays.toString(OrderStatus.values());
        log.debug("exception.order.type_mismatch: status=400");
        return apiErrorResponseCreator.buildResponse(message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleOrderNotFound(final OrderNotFoundException ex) {
        log.debug("exception.order.not_found: message={}", ex.getMessage());
        return apiErrorResponseCreator.buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OrderAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiErrorResponse handleAccessDenied(final OrderAccessDeniedException ex) {
        log.debug("exception.order.access_denied: message={}", ex.getMessage());
        return apiErrorResponseCreator.buildResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(InvalidOrderStateTransitionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleInvalidTransition(final InvalidOrderStateTransitionException ex) {
        log.debug("exception.order.invalid_transition: message={}", ex.getMessage());
        return apiErrorResponseCreator.buildResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(OrderCancellationWindowExpiredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleCancellationExpired(final OrderCancellationWindowExpiredException ex) {
        log.debug("exception.order.cancellation_expired: message={}", ex.getMessage());
        return apiErrorResponseCreator.buildResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }
}
