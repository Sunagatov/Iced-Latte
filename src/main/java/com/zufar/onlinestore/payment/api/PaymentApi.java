package com.zufar.onlinestore.payment.api;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.zufar.onlinestore.payment.dto.CreatePaymentDto;
import com.zufar.onlinestore.payment.dto.CreatePaymentMethodDto;
import com.zufar.onlinestore.payment.dto.PaymentDetailsDto;
import com.zufar.onlinestore.payment.dto.PaymentDetailsWithTokenDto;
import com.zufar.onlinestore.payment.exception.PaymentNotFoundException;

public interface PaymentApi {

    /**
     * This method allows to create a payment object
     *
     * @param createPaymentDto the request dto to create a payment object
     * @return PaymentDetailsWithTokenDto combines payment details and a payment token for payment processing on the front end side
     * @throws StripeException this is the parent of all Stripe API exceptions, in this method, exceptions descendants related to the payment creation and processing
     * */
    PaymentDetailsWithTokenDto createPayment(final CreatePaymentDto createPaymentDto) throws StripeException;

    /**
     * This method allows to create a payment method object
     *
     * @param createPaymentMethodDto the request dto to create a payment method object
     * @return String payment method identifier, for secure method transfer using the Stripe API
     * @throws StripeException this is the parent of all Stripe API exceptions, in this method, exceptions descendants related to the payment method creation and processing
     * */
    String createPaymentMethod(final CreatePaymentMethodDto createPaymentMethodDto) throws StripeException;

    /**
     * This method allows to create a payment method object
     *
     * @param paymentId the payment identifier to search payment details
     * @return PaymentDetailsDto these are payment details
     * @throws PaymentNotFoundException this exception throw then payment wasn't found
     * */
    PaymentDetailsDto getPaymentDetails(final Long paymentId) throws PaymentNotFoundException;

    /**
     * This method allows to create a payment method object
     *
     * @param paymentIntentPayload this param it is a string describing of the payment intent event type.
     * @param stripeSignatureHeader this param it is a string describing of the stripe signature, which provide safe work with Stripe API webhooks mechanism
     * @throws SignatureVerificationException this exception throw then stripe signature verification problem was occurred
     * */
    void paymentEventProcess(final String paymentIntentPayload, final String stripeSignatureHeader) throws SignatureVerificationException;

}