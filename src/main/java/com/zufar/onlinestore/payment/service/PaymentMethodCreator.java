package com.zufar.onlinestore.payment.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentMethodCreateParams;
import com.zufar.onlinestore.payment.config.StripeConfiguration;
import com.zufar.onlinestore.payment.converter.PaymentMethodConverter;
import com.zufar.onlinestore.payment.dto.CreatePaymentMethodDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentMethodCreator {

    private final StripeConfiguration stripeConfig;
    private final PaymentMethodConverter paymentMethodConverter;

    public String createPaymentMethod(final CreatePaymentMethodDto createPaymentMethodDto) throws StripeException {
        Stripe.apiKey = stripeConfig.publishableKey();

        PaymentMethodCreateParams params = paymentMethodConverter.toPaymentMethodParams(createPaymentMethodDto);
        PaymentMethod paymentMethod = PaymentMethod.create(params);
        log.info("Created payment method: payment method has been created: paymentMethod: {}.", paymentMethod);

        return paymentMethod.getId();
    }
}
