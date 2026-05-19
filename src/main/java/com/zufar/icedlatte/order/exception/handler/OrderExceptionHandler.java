package com.zufar.icedlatte.order.exception.handler;

import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.common.exception.ProblemType;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.exception.InvalidOrderStateTransitionException;
import com.zufar.icedlatte.order.exception.OrderAccessDeniedException;
import com.zufar.icedlatte.order.exception.OrderCancellationWindowExpiredException;
import com.zufar.icedlatte.order.exception.OrderException;
import com.zufar.icedlatte.order.exception.OrderNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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
        return problemDetailFactory.build(ProblemType.INVALID_PARAMETER, "Invalid parameter",
                HttpStatus.BAD_REQUEST, "Incorrect status value. Supported: " + Arrays.toString(OrderStatus.values()));
    }

    @ExceptionHandler(OrderException.class)
    public ResponseEntity<ProblemDetail> handleOrderException(final OrderException ex) {
        record ErrorMapping(String logTag, String typeSlug, String title, HttpStatus status, String detail) {}

        var mapping = switch (ex) {
            case OrderNotFoundException _ ->
                    new ErrorMapping("exception.order.not_found", ProblemType.ORDER_NOT_FOUND, "Order not found", HttpStatus.NOT_FOUND, "Order not found.");
            case OrderAccessDeniedException _ ->
                    new ErrorMapping("exception.order.access_denied", ProblemType.ORDER_ACCESS_DENIED, "Access denied", HttpStatus.FORBIDDEN, "Access denied.");
            case InvalidOrderStateTransitionException _ ->
                    new ErrorMapping("exception.order.invalid_transition", ProblemType.ORDER_STATE_INVALID, "Invalid order state", HttpStatus.CONFLICT, "This order can no longer be modified.");
            case OrderCancellationWindowExpiredException _ ->
                    new ErrorMapping("exception.order.cancellation_expired", ProblemType.ORDER_CANCELLATION_EXPIRED, "Cancellation window expired", HttpStatus.CONFLICT, "Order cannot be cancelled: cancellation window has expired.");
        };

        log.debug("{}: status={}", mapping.logTag(), mapping.status().value());
        ProblemDetail pd = problemDetailFactory.build(mapping.typeSlug(), mapping.title(), mapping.status(), mapping.detail());
        return ResponseEntity.status(mapping.status()).body(pd);
    }
}
