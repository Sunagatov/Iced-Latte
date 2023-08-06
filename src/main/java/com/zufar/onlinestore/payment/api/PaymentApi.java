package com.zufar.onlinestore.payment.api;

import com.stripe.exception.SignatureVerificationException;
import com.zufar.onlinestore.payment.dto.CreatePaymentDto;
import com.zufar.onlinestore.payment.dto.CreatePaymentMethodDto;
import com.zufar.onlinestore.payment.dto.PaymentDetailsDto;
import com.zufar.onlinestore.payment.dto.PaymentDetailsWithTokenDto;
import com.zufar.onlinestore.payment.exception.*;

public interface PaymentApi {

    /**
     * This method allows to create a payment object
     *
     * @param createPaymentDto the request dto to create a payment object
     * @return PaymentDetailsWithTokenDto combines payment details and a payment token for payment processing on the front end side
     * @throws PaymentIntentProcessingException this error occurs in cases where the data passed to process a payment intent is not valid.
     * */
    PaymentDetailsWithTokenDto createPayment(final CreatePaymentDto createPaymentDto) throws PaymentIntentProcessingException;

    /**
     * This method allows to create a payment method object
     *
     * @param createPaymentMethodDto the request dto to create a payment method object
     * @return String payment method identifier, for secure method transfer using the Stripe API
     * @throws PaymentMethodProcessingException this error occurs in cases when the data transmitted to create a payment method is not valid.
     * */
    String createPaymentMethod(final CreatePaymentMethodDto createPaymentMethodDto) throws PaymentMethodProcessingException;

    /**
     * This method allows to create a payment method object
     *
     * @param paymentId the payment identifier to search payment details
     * @return PaymentDetailsDto these are payment details
     * @throws PaymentNotFoundException this error is thrown in cases when the payment by the passed identifier was not found
     * */
    PaymentDetailsDto getPaymentDetails(final Long paymentId) throws PaymentNotFoundException;

    /**
     * This method allows to create a payment method object
     *
     * @param paymentIntentPayload this param it is a string describing of the payment intent event type.
     * @param stripeSignatureHeader this param it is a string describing of the stripe signature, which provide safe work with Stripe API webhooks mechanism
     * @throws PaymentEventProcessingException this error occurs in cases where webhook event data is not valid.
     * @throws PaymentEventParsingException this error occurs when it is impossible to start the event in the intent due to invalid data
     * */
    void processPaymentEvent(final String paymentIntentPayload, final String stripeSignatureHeader) throws PaymentEventProcessingException, PaymentEventParsingException;

}