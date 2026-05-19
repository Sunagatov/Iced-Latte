package com.zufar.icedlatte.payment.exception.handler;

import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.common.exception.ProblemType;
import com.zufar.icedlatte.payment.exception.PaymentEventProcessingException;
import com.zufar.icedlatte.payment.exception.PaymentException;
import com.zufar.icedlatte.payment.exception.StripeSessionCreationException;
import com.stripe.exception.AuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Order(0)
public class PaymentExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ProblemDetail> handlePaymentException(final PaymentException ex) {
        record ErrorMapping(String logTag, String typeSlug, String title, HttpStatus status, String detail) {}

        var mapping = switch (ex) {
            case PaymentEventProcessingException _ ->
                    new ErrorMapping("exception.payment.event_failed", ProblemType.PAYMENT_EVENT_FAILED, "Payment event failed", HttpStatus.BAD_REQUEST, "Payment event could not be verified.");
            case StripeSessionCreationException e -> {
                if (e.getCause() instanceof AuthenticationException) {
                    log.error("payment.session.failed: reason=invalid_stripe_key, status=502", e);
                } else {
                    log.warn("payment.session.failed: exceptionClass={}, status=502", e.getClass().getSimpleName());
                }
                yield new ErrorMapping("exception.payment.session_failed", ProblemType.PAYMENT_SESSION_FAILED,
                        "Payment session failed", HttpStatus.BAD_GATEWAY, "Payment session could not be created.");
            }
        };

        HttpStatus httpStatus = mapping.status();
        log.debug("{}: status={}", mapping.logTag(), httpStatus.value());
        return ResponseEntity.status(httpStatus)
                .body(problemDetailFactory.build(mapping.typeSlug(), mapping.title(), httpStatus, mapping.detail()));
    }
}
