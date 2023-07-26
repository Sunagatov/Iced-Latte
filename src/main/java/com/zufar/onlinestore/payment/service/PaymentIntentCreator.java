package com.zufar.onlinestore.payment.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.zufar.onlinestore.payment.config.StripeConfiguration;
import com.zufar.onlinestore.payment.converter.PaymentIntentConverter;
import com.zufar.onlinestore.payment.dto.CreatePaymentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentIntentCreator {

    private final StripeConfiguration stripeConfig;
    private final PaymentIntentConverter paymentIntentConverter;

    public PaymentIntent createPaymentIntent(final CreatePaymentDto createPaymentDto) throws StripeException {
        Stripe.apiKey = stripeConfig.secretKey();

        PaymentIntentCreateParams params = paymentIntentConverter.toPaymentIntentParams(createPaymentDto);
        log.info("Create payment intent: payment intent params: {} for creation.", params);

        return PaymentIntent.create(params);
    }

}
