package com.zufar.icedlatte.payment.exception.handler;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.payment.exception.PaymentEventProcessingException;
import com.zufar.icedlatte.payment.exception.StripeSessionCreationException;
import com.zufar.icedlatte.payment.exception.StripeSessionIsNotComplete;
import com.stripe.exception.AuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Order(0)
public class PaymentExceptionHandler {

    private final ApiErrorResponseCreator apiErrorResponseCreator;

    @ExceptionHandler(PaymentEventProcessingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @SuppressWarnings("unused")
    public ApiErrorResponse handlePaymentEventProcessingException(final PaymentEventProcessingException e) {
        return apiErrorResponseCreator.buildResponse(e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(StripeSessionCreationException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @SuppressWarnings("unused")
    public ApiErrorResponse handleStripeSessionCreationException(final StripeSessionCreationException e) {
        ApiErrorResponse response = apiErrorResponseCreator.buildResponse(e, HttpStatus.BAD_GATEWAY);
        Throwable cause = e.getCause();
        if (cause instanceof AuthenticationException) {
            log.error("payment.session.failed: reason=invalid_stripe_key, status=502", e);
        } else {
            log.warn("payment.session.failed: exceptionClass={}, status=502", e.getClass().getSimpleName());
        }
        return response;
    }

    @ExceptionHandler(StripeSessionIsNotComplete.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @SuppressWarnings("unused")
    public ApiErrorResponse handleStripeSessionIsNotComplete(final StripeSessionIsNotComplete e) {
        return apiErrorResponseCreator.buildResponse(e, HttpStatus.BAD_REQUEST);
    }
}
