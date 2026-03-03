package com.zufar.icedlatte.payment.exception.handler;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.payment.exception.PaymentEventProcessingException;
import com.zufar.icedlatte.payment.exception.StripeSessionCreationException;
import com.zufar.icedlatte.payment.exception.StripeSessionIsNotComplete;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class PaymentExceptionHandler {

    private final ApiErrorResponseCreator apiErrorResponseCreator;

    @ExceptionHandler(PaymentEventProcessingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handlePaymentEventProcessingException(final PaymentEventProcessingException e) {
        ApiErrorResponse response = apiErrorResponseCreator.buildResponse(e, HttpStatus.BAD_REQUEST);
        log.warn("exception.payment.event_processing: message={}", response.message());
        return response;
    }

    @ExceptionHandler(StripeSessionCreationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleStripeSessionCreationException(final StripeSessionCreationException e) {
        ApiErrorResponse response = apiErrorResponseCreator.buildResponse(e, HttpStatus.BAD_REQUEST);
        log.warn("payment.session.failed: message={}", response.message());
        return response;
    }

    @ExceptionHandler(StripeSessionIsNotComplete.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleStripeSessionIsNotComplete(final StripeSessionIsNotComplete e) {
        ApiErrorResponse response = apiErrorResponseCreator.buildResponse(e, HttpStatus.BAD_REQUEST);
        log.warn("exception.payment.session_incomplete: message={}", response.message());
        return response;
    }
}
