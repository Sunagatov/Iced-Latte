package com.zufar.onlinestore.payment.api;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentDetailsDto;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentWithClientSecretDto;
import com.zufar.onlinestore.payment.exception.PaymentNotFoundException;

public interface PaymentApi {

    /**
     * This method is responsible for the processing of the payment
     *
     * @param cardInfoTokenId
     * @return PaymentDetailsWithTokenDto combines payment details and a payment token for payment processing on the front end side
     * */
    ProcessedPaymentWithClientSecretDto processPayment(final String cardInfoTokenId) throws StripeException;

    /**
     * This method allows to create a payment method object
     *
     * @param paymentId the payment identifier to search payment details
     * @return PaymentDetailsDto these are payment details
     * */
    ProcessedPaymentDetailsDto getPaymentDetails(final Long paymentId) throws PaymentNotFoundException;

    /**
     * This method allows to create a payment method object
     *
     * @param paymentIntentPayload this param it is a string describing of the payment intent event type.
     * @param stripeSignatureHeader this param it is a string describing of the stripe signature, which provide safe work with Stripe API webhooks mechanism
     * */
    void processPaymentEvent(final String paymentIntentPayload, final String stripeSignatureHeader) throws SignatureVerificationException;

}