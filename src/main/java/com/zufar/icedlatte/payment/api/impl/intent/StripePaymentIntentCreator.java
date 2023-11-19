package com.zufar.icedlatte.payment.api.impl.intent;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentCreateParams;
import com.zufar.icedlatte.openapi.dto.ShoppingSessionDto;
import com.zufar.icedlatte.payment.config.StripeConfiguration;
import com.zufar.icedlatte.payment.converter.StripePaymentIntentConverter;
import com.zufar.icedlatte.payment.exception.PaymentIntentProcessingException;
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
    private final StripeConfiguration stripeConfiguration;

    public PaymentIntent createStripePaymentIntent(final PaymentMethod paymentMethod, ShoppingSessionDto shoppingSession) {
        log.info("Create stripe payment intent: starting: start payment intent creation");
        String currency = stripeConfiguration.currency();
        PaymentIntentCreateParams paymentIntentCreateParams = stripePaymentIntentConverter.toStripeObject(paymentMethod, shoppingSession, currency);
        String paymentMethodId = paymentIntentCreateParams.getPaymentMethod();
        log.info("Create stripe payment intent: in progress: creation stripe payment intent with paymentMethodId = {}", paymentMethodId);
        try {
            PaymentIntent paymentIntent = PaymentIntent.create(paymentIntentCreateParams);
            log.info("Create stripe payment intent: successful: stripe payment intent was created with id = {}", paymentIntent.getId());
            return paymentIntent;
        } catch (StripeException ex) {
            log.error("Create stripe payment intent: failed: stripe payment intent was not created");
            throw new PaymentIntentProcessingException(paymentMethodId);
        }
    }
}
