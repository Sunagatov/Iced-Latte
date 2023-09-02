package com.zufar.onlinestore.payment.exception.handler;

import com.zufar.onlinestore.common.exception.handler.GlobalExceptionHandler;
import com.zufar.onlinestore.common.response.ApiResponse;
import com.zufar.onlinestore.payment.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RequiredArgsConstructor
@RestControllerAdvice
@Slf4j
public class PaymentExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handlePaymentNotFoundException(final PaymentNotFoundException exception) {
        ApiResponse<Void> apiResponse = buildResponse(exception, HttpStatus.NOT_FOUND);
        log.error("Handle payment not found exception: failed: messages: {}, description: {}.",
                apiResponse.messages(), apiResponse.description());

        return apiResponse;
    }

    @ExceptionHandler(PaymentEventProcessingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handlePaymentEventProcessingException(final PaymentEventProcessingException exception) {
        ApiResponse<Void> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle payment event processing exception: failed: messages: {}, description: {}.",
                apiResponse.messages(), apiResponse.description());

        return apiResponse;
    }

    @ExceptionHandler(PaymentIntentProcessingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handlePaymentIntentProcessingException(final PaymentIntentProcessingException exception) {
        ApiResponse<Void> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle payment intent processing exception: failed: messages: {}, description: {}.",
                apiResponse.messages(), apiResponse.description());

        return apiResponse;
    }

    @ExceptionHandler(PaymentMethodProcessingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handlePaymentMethodProcessingException(final PaymentMethodProcessingException exception) {
        ApiResponse<Void> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle payment method processing exception: failed: messages: {}, description: {}.",
                apiResponse.messages(), apiResponse.description());

        return apiResponse;
    }

    @ExceptionHandler(PaymentEventParsingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handlePaymentEventParsingException(final PaymentEventParsingException exception) {
        ApiResponse<Void> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle payment event parsing exception: failed: messages: {}, description: {}.",
                apiResponse.messages(), apiResponse.description());

        return apiResponse;
    }

    @ExceptionHandler(CardTokenCreationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleCardTokenCreationException(final CardTokenCreationException exception) {
        ApiResponse<Void> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle card token creation exception: failed: messages: {}, description: {}.",
                apiResponse.messages(), apiResponse.description());

        return apiResponse;
    }

    @ExceptionHandler(CardTokenRetrievingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleCardTokenRetrievingException(final CardTokenRetrievingException exception) {
        ApiResponse<Void> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle card token retrieving exception: failed: messages: {}, description: {}.",
                apiResponse.messages(), apiResponse.description());

        return apiResponse;
    }

    @ExceptionHandler(CustomerRetrievingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleCustomerRetrievingException(final CustomerRetrievingException exception) {
        ApiResponse<Void> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle customer retrieving exception: failed: messages: {}, description: {}.",
                apiResponse.messages(), apiResponse.description());

        return apiResponse;
    }

    @ExceptionHandler(PaymentMethodNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handlePaymentMethodNotFoundException(final PaymentMethodNotFoundException exception) {
        ApiResponse<Void> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle payment method not found exception: failed: messages: {}, description: {}.",
                apiResponse.messages(), apiResponse.description());

        return apiResponse;
    }

    @ExceptionHandler(PaymentMethodRetrievingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handlePaymentMethodRetrievingException(final PaymentMethodRetrievingException exception) {
        ApiResponse<Void> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle payment method retrieving exception: failed: messages: {}, description: {}.",
                apiResponse.messages(), apiResponse.description());

        return apiResponse;
    }

    @ExceptionHandler(ShoppingSessionAlreadyPaidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleShoppingSessionAlreadyPaidException(final ShoppingSessionAlreadyPaidException exception) {
        ApiResponse<Void> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle shopping session already paid exception: failed: messages: {}, description: {}.",
                apiResponse.messages(), apiResponse.description());

        return apiResponse;
    }
}
