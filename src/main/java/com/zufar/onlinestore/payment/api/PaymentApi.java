package com.zufar.onlinestore.payment.api;

import com.zufar.onlinestore.openapi.dto.CreateCardDetailsTokenRequest;
import com.zufar.onlinestore.openapi.dto.ProcessedPaymentDetailsDto;
import com.zufar.onlinestore.openapi.dto.ProcessedPaymentWithClientSecretDto;

public interface PaymentApi {

    /**
     * This method is responsible for payment processing
     *
     * @param cardDetailsTokenId is stripe token collected based on information about the user's payment card
     * @return PaymentDetailsWithTokenDto combines payment identifier and payment token for processing on front-end side
     * */
    ProcessedPaymentWithClientSecretDto processPayment(final String cardDetailsTokenId);

    /**
     * This method allows to create a payment method object
     *
     * @param paymentId the payment identifier for retrieve payment details
     * @return PaymentDetailsDto is payment details object
     * */
    ProcessedPaymentDetailsDto getPaymentDetails(final Long paymentId);

    /**
     * This method allows to create a payment method object
     *
     * @param paymentIntentPayload string describing of the payment intent event type.
     * @param stripeSignatureHeader stripe signature, which provide safe work with Stripe API webhooks mechanism
     * */
    void processPaymentEvent(final String paymentIntentPayload, final String stripeSignatureHeader);

    /**
     *  This method is a temporary solution until the front-end goes functional to pass us a token with payment card details.
     *  In the meantime, this method serves to test the payment api.
     *
     * @param createCardDetailsTokenRequest object that contains data about customer payment card.
     * @return returns card details token in string form
     * */
    String processCardDetailsToken(final CreateCardDetailsTokenRequest createCardDetailsTokenRequest);
}