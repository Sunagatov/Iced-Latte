package com.zufar.onlinestore.payment.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentCreateParams;
import com.zufar.onlinestore.payment.config.StripeConfiguration;
import com.zufar.onlinestore.payment.enums.PaymentConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentIntentCreator {

    private final StripeConfiguration stripeConfig;

    public PaymentIntent createPaymentIntent(String paymentMethodId, BigDecimal totalPrice, String currency) throws StripeException {
        Stripe.apiKey = stripeConfig.secretKey();

        PaymentMethod paymentMethod = getPaymentMethod(paymentMethodId);
        PaymentIntentCreateParams params = fillPaymentIntentParams(paymentMethod, totalPrice, currency);
        log.info("create payment intent: fill payment intent params for payment intent creation: params: {}.", params);

        return PaymentIntent.create(params);
    }

    private PaymentMethod getPaymentMethod(String paymentMethodId) throws StripeException {
        return PaymentMethod.retrieve(paymentMethodId);
    }

    private PaymentIntentCreateParams fillPaymentIntentParams(PaymentMethod paymentMethod, BigDecimal totalPrice, String currency) {
        String email = paymentMethod.getBillingDetails().getEmail();
        Integer delimiter = PaymentConstants.PAYMENT_DELIMITER.getValue();
        return PaymentIntentCreateParams.builder()
                .setAmount(totalPrice.longValue() * delimiter)
                .setCurrency(currency)
                .setPaymentMethod(paymentMethod.getId())
                .setReceiptEmail(email)
                .build();
    }
}
