package com.zufar.onlinestore.payment.api.impl.intent;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentMethodCreateParams;
import com.zufar.onlinestore.payment.exception.PaymentMethodProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.stripe.param.PaymentMethodCreateParams.Token;
import static com.stripe.param.PaymentMethodCreateParams.Type;

/**
 * This class is responsible for converting passed parameters and creating based
 * on their payment method (stripe object). Payment method is secondary object for
 * creating and processing payment by Stripe API.
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class StripePaymentMethodCreator {

    public PaymentMethod createStripePaymentMethod(final String cardDetailsToken) throws PaymentMethodProcessingException {
        PaymentMethodCreateParams paymentMethodCreateParams = PaymentMethodCreateParams.builder()
                .setType(Type.CARD)
                .setCard(Token.builder()
                        .setToken(cardDetailsToken)
                        .build())
                .build();
        String paymentMethodType = paymentMethodCreateParams.getType().getValue();
        log.info("Create payment method: in progress: creation stripe payment method with type: {}.", paymentMethodType);
        try {
            PaymentMethod paymentMethod = PaymentMethod.create(paymentMethodCreateParams);
            log.info("Create payment method: successful: stripe payment method was created with id: {}.", paymentMethod.getId());
            return paymentMethod;
        } catch (StripeException ex) {
            log.info("Create payment method: failed: stripe payment method was not created.");
            throw new PaymentMethodProcessingException(paymentMethodCreateParams.getType().getValue());
        }
    }
}
