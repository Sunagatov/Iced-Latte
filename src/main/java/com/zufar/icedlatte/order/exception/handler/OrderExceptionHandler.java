package com.zufar.icedlatte.order.exception.handler;

import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
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
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Arrays;

@Slf4j
@RestControllerAdvice(basePackages = {"com.zufar.icedlatte.order.endpoint"})
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class OrderExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleTypeMismatch(final MethodArgumentTypeMismatchException ignored) {
        log.debug("exception.order.type_mismatch: status=400");
        return problemDetailFactory.build("invalid-parameter", "Invalid parameter",
                HttpStatus.BAD_REQUEST, "Incorrect status value. Supported: " + Arrays.toString(OrderStatus.values()));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleOrderNotFound(final OrderNotFoundException ex) {
        log.debug("exception.order.not_found: status=404");
        return problemDetailFactory.build("order-not-found", "Order not found",
                HttpStatus.NOT_FOUND, "Order not found.");
    }

    @ExceptionHandler(OrderAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleAccessDenied(final OrderAccessDeniedException ex) {
        log.debug("exception.order.access_denied: status=403");
        return problemDetailFactory.build("order-access-denied", "Access denied",
                HttpStatus.FORBIDDEN, "Access denied.");
    }

    @ExceptionHandler(InvalidOrderStateTransitionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleInvalidTransition(final InvalidOrderStateTransitionException ex) {
        log.debug("exception.order.invalid_transition: status=409");
        return problemDetailFactory.build("order-state-invalid", "Invalid order state",
                HttpStatus.CONFLICT, "This order can no longer be modified.");
    }

    @ExceptionHandler(OrderCancellationWindowExpiredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleCancellationExpired(final OrderCancellationWindowExpiredException ex) {
        log.debug("exception.order.cancellation_expired: status=409");
        return problemDetailFactory.build("order-cancellation-expired", "Cancellation window expired",
                HttpStatus.CONFLICT, "Order cannot be cancelled: cancellation window has expired.");
    }
}
