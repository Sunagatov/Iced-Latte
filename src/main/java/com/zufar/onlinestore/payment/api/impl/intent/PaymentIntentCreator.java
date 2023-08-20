package com.zufar.onlinestore.payment.api.impl.intent;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.zufar.onlinestore.payment.config.StripeConfiguration;
import com.zufar.onlinestore.payment.converter.PaymentIntentConverter;
import com.zufar.onlinestore.payment.dto.CreatePaymentDto;
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
public class PaymentIntentCreator {

    private final StripeConfiguration stripeConfig;
    private final PaymentIntentConverter paymentIntentConverter;

    public PaymentIntent createPaymentIntent(final CreatePaymentDto createPaymentDto) throws PaymentIntentProcessingException {
        String stripeKey = stripeConfig.secretKey();
        StripeConfiguration.setStripeKey(stripeKey);

        PaymentIntentCreateParams params = paymentIntentConverter.toPaymentIntentParams(createPaymentDto);
        log.info("Create payment intent: payment intent params: {} for creation.", params);

        try {
            return PaymentIntent.create(params);
        } catch (StripeException ex) {
            log.error("Error during Payment processing", ex);
            throw new PaymentIntentProcessingException(params.getPaymentMethod());
        }
    }

}
