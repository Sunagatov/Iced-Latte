package com.zufar.onlinestore.payment.api;

import com.stripe.exception.StripeException;
import com.zufar.onlinestore.payment.api.dto.CreateCardDetailsTokenDto;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentDetailsDto;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentWithClientSecretDto;
import com.zufar.onlinestore.payment.exception.PaymentEventParsingException;
import com.zufar.onlinestore.payment.exception.PaymentEventProcessingException;
import com.zufar.onlinestore.payment.exception.PaymentIntentProcessingException;
import com.zufar.onlinestore.payment.exception.PaymentNotFoundException;
import com.zufar.onlinestore.payment.exception.PaymentMethodProcessingException;
import com.zufar.onlinestore.payment.exception.StripeCustomerProcessingException;

public interface PaymentApi {

    /**
     * This method is responsible for payment processing
     *
     * @param cardDetailsTokenId is stripe token collected based on information about the user's payment card
     * @return PaymentDetailsWithTokenDto combines payment identifier and payment token for processing on front-end side
     * @throws PaymentMethodProcessingException thrown when payment method processing cannot be completed successfully
     * @throws StripeCustomerProcessingException thrown when customer creation cannot be completed successfully
     * @throws PaymentIntentProcessingException thrown when payment intent processing cannot be completed successfully
     * */
    ProcessedPaymentWithClientSecretDto processPayment(final String cardDetailsTokenId) throws PaymentMethodProcessingException, StripeCustomerProcessingException, PaymentIntentProcessingException;

    /**
     * This method allows to create a payment method object
     *
     * @param paymentId the payment identifier for retrieve payment details
     * @return PaymentDetailsDto is payment details object
     * @throws PaymentNotFoundException thrown when payment details cannot be retrieved successfully
     * */
    ProcessedPaymentDetailsDto getPaymentDetails(final Long paymentId) throws PaymentNotFoundException;

    /**
     * This method allows to create a payment method object
     *
     * @param paymentIntentPayload string describing of the payment intent event type.
     * @param stripeSignatureHeader stripe signature, which provide safe work with Stripe API webhooks mechanism
     * @throws PaymentEventProcessingException thrown when payment event processing cannot be completed successfully
     * @throws PaymentEventParsingException thrown when payment event parsing cannot be completed successfully
     * */
    void processPaymentEvent(final String paymentIntentPayload, final String stripeSignatureHeader) throws PaymentEventProcessingException, PaymentEventParsingException;

    /**
     *  This method is a temporary solution until the front-end goes functional to pass us a token with payment card details.
     *  In the meantime, this method serves to test the payment api.
     *
     * @param createCardDetailsTokenDto object that contains data about customer payment card.
     * @return returns card details token in string form
     * */
    String processCardDetailsToken(final CreateCardDetailsTokenDto createCardDetailsTokenDto) throws StripeException;
}