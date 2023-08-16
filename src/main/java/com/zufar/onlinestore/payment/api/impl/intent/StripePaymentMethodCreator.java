package com.zufar.onlinestore.payment.api.impl.intent;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Token;
import com.stripe.param.PaymentMethodCreateParams;
import com.zufar.onlinestore.payment.converter.StripePaymentMethodConverter;
import com.zufar.onlinestore.payment.exception.PaymentMethodProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for converting passed parameters and creating based
 * on their payment method (stripe object). Payment method is secondary object for
 * creating and processing payment by Stripe API.
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class StripePaymentMethodCreator {

    private final StripePaymentMethodConverter stripePaymentMethodConverter;

    public PaymentMethod createStripePaymentMethod(final String cardDetailsTokenId) throws PaymentMethodProcessingException {
        String paymentMethodType = Strings.EMPTY;
        try {
            Token retrievedCardDetailsToken = Token.retrieve(cardDetailsTokenId);
            PaymentMethodCreateParams paymentMethodCreateParams = stripePaymentMethodConverter.toStripeObject(retrievedCardDetailsToken);
            paymentMethodType = paymentMethodCreateParams.getType().getValue();
            log.info("Create payment method: in progress: creation stripe payment method with type = {}.", paymentMethodType);

            PaymentMethod paymentMethod = PaymentMethod.create(paymentMethodCreateParams);
            log.info("Create payment method: successful: stripe payment method was created with id = {}.", paymentMethod.getId());
            return paymentMethod;
        } catch (StripeException ex) {
            log.info("Create payment method: failed: stripe payment method was not created.");
            throw new PaymentMethodProcessingException(paymentMethodType);
        }
    }
}
