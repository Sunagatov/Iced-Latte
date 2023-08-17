package com.zufar.onlinestore.payment.api.impl.intent;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentCreateParams;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.payment.converter.StripePaymentIntentConverter;
import com.zufar.onlinestore.payment.exception.PaymentIntentProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for converting passed parameters and creating based
 * on their payment intent (stripe object). Payment intent is the main object for
 * creating and processing payment by Stripe API.
 * */

@Slf4j
@RequiredArgsConstructor
@Service
public class StripePaymentIntentCreator {

    private final StripePaymentIntentConverter stripePaymentIntentConverter;

    public PaymentIntent createStripePaymentIntent(final PaymentMethod paymentMethod, ShoppingSessionDto shoppingSession) throws PaymentIntentProcessingException {
        log.info("Create payment intent: starting: creation of payment intent was started");
        PaymentIntentCreateParams paymentIntentCreateParams = stripePaymentIntentConverter.toStripeObject(paymentMethod, shoppingSession);
        String paymentMethodId = paymentIntentCreateParams.getPaymentMethod();
        log.info("Create payment intent: in progress: creation stripe payment intent with payment method Id: {}", paymentMethodId);
        try {
            PaymentIntent paymentIntent = PaymentIntent.create(paymentIntentCreateParams);
            log.info("Create payment intent: successful: stripe payment intent was created with Id: {}", paymentIntent.getId());
            return paymentIntent;
        } catch (StripeException ex) {
            log.error("Create payment intent: failed: stripe payment intent was not created");
            throw new PaymentIntentProcessingException(paymentMethodId);
        }
    }
}
