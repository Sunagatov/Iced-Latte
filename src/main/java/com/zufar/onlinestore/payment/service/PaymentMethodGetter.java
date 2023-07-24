package com.zufar.onlinestore.payment.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentMethodCreateParams;
import com.zufar.onlinestore.payment.config.StripeConfiguration;
import com.zufar.onlinestore.payment.dto.CreatePaymentMethodDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentMethodGetter {

    private final StripeConfiguration stripeConfig;

    public String getPaymentMethodId(CreatePaymentMethodDto paymentRequest) throws StripeException {
        Stripe.apiKey = stripeConfig.publishableKey();

        PaymentMethodCreateParams params = fillPaymentMethodParams(paymentRequest);
        PaymentMethod paymentMethod = PaymentMethod.create(params);

        return paymentMethod.getId();
    }

    private  PaymentMethodCreateParams fillPaymentMethodParams(CreatePaymentMethodDto paymentRequest) {
        return PaymentMethodCreateParams.builder()
                .setCard(PaymentMethodCreateParams.CardDetails.builder()
                        .setNumber(paymentRequest.cardNumber())
                        .setExpMonth(paymentRequest.expMonth())
                        .setExpYear(paymentRequest.expYear())
                        .setCvc(paymentRequest.cvc())
                        .build())
                .setType(PaymentMethodCreateParams.Type.CARD)
                .build();
    }
}
