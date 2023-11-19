package com.zufar.icedlatte.payment.api.impl.customer;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Token;
import com.stripe.param.PaymentMethodCreateParams;
import com.zufar.icedlatte.payment.converter.StripePaymentMethodConverter;
import com.zufar.icedlatte.payment.exception.CardTokenRetrievingException;
import com.zufar.icedlatte.payment.exception.PaymentMethodProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for converting passed parameters and creating based
 * on their payment method (stripe object). Payment method is secondary object for
 * creating and processing payment by Stripe API.
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class StripePaymentMethodProcessor {

    private final StripePaymentMethodConverter stripePaymentMethodConverter;

    public PaymentMethod processStripePaymentMethod(final String cardDetailsTokenId) {
        Token retrievedToken = retrieveCardToken(cardDetailsTokenId);

        return createStripePaymentMethod(retrievedToken);
    }

    private PaymentMethod createStripePaymentMethod(Token retrievedToken) {
        String paymentMethodType = "";
        try {
            log.info("Create stripe payment method: in progress: start creation stripe payment method");
            PaymentMethodCreateParams paymentMethodCreateParams = stripePaymentMethodConverter.toStripeObject(retrievedToken);
            paymentMethodType = paymentMethodCreateParams.getType().getValue();
            PaymentMethod paymentMethod = PaymentMethod.create(paymentMethodCreateParams);
            String paymentMethodId = paymentMethod.getId();
            log.info("Create stripe payment method: successfully: stripe payment method was created with paymentMethodId = {}.", paymentMethodId);

            return paymentMethod;

        } catch (StripeException ex) {
            log.info("Create stripe payment method: failed: stripe payment method was not created.");
            throw new PaymentMethodProcessingException(paymentMethodType);
        }
    }

    private Token retrieveCardToken(String cardDetailsTokenId) {
        try {
            log.info("Retrieve card token: in progress: start to retrieve card token by cardDetailsTokenId = {}.", cardDetailsTokenId);
            Token retrievedCardToken = Token.retrieve(cardDetailsTokenId);
            log.info("Retrieve card token: successfully: card token was retrieved with id = {}.", retrievedCardToken.getId());

            return retrievedCardToken;

        } catch (StripeException e) {
            log.error("Retrieve card token: failed: card token cannot retrieve");
            throw new CardTokenRetrievingException(cardDetailsTokenId);
        }
    }
}
