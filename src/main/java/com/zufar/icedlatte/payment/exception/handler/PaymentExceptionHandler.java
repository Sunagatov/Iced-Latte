package com.zufar.icedlatte.payment.exception.handler;

import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import com.zufar.icedlatte.common.exception.ProblemType;
import com.zufar.icedlatte.payment.exception.PaymentEventProcessingException;
import com.zufar.icedlatte.payment.exception.StripeSessionCreationException;
import com.stripe.exception.AuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Order(0)
public class PaymentExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    @ExceptionHandler(PaymentEventProcessingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @SuppressWarnings("unused")
    public ProblemDetail handlePaymentEventProcessingException(final PaymentEventProcessingException e) {
        return problemDetailFactory.build(ProblemType.PAYMENT_EVENT_FAILED, "Payment event failed",
                HttpStatus.BAD_REQUEST, "Payment event could not be verified.");
    }

    @ExceptionHandler(StripeSessionCreationException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @SuppressWarnings("unused")
    public ProblemDetail handleStripeSessionCreationException(final StripeSessionCreationException e) {
        Throwable cause = e.getCause();
        if (cause instanceof AuthenticationException) {
            log.error("payment.session.failed: reason=invalid_stripe_key, status=502", e);
        } else {
            log.warn("payment.session.failed: exceptionClass={}, status=502", e.getClass().getSimpleName());
        }
        return problemDetailFactory.build(ProblemType.PAYMENT_SESSION_FAILED, "Payment session failed",
                HttpStatus.BAD_GATEWAY, "Payment session could not be created.");
    }
}
