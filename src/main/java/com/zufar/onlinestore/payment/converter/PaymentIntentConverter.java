package com.zufar.onlinestore.payment.converter;

import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.zufar.onlinestore.payment.calculator.PaymentPriceCalculator;
import com.zufar.onlinestore.payment.dto.CreatePaymentDto;
import com.zufar.onlinestore.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PaymentIntentConverter {

    private final PaymentPriceCalculator paymentPriceCalculator;

    public Payment toPayment(final PaymentIntent paymentIntent) {
        return Payment.builder()
                .itemsTotalPrice(paymentPriceCalculator.calculatePriceForPayment(paymentIntent.getAmount()))
                .paymentIntentId(paymentIntent.getId())
                .currency(paymentIntent.getCurrency())
                .build();
    }

    public PaymentIntentCreateParams toPaymentIntentParams(final CreatePaymentDto createPaymentDto) {
        return PaymentIntentCreateParams.builder()
                .setAmount(paymentPriceCalculator.calculatePriceForPaymentIntent(
                        createPaymentDto.priceDetails().totalPrice()))
                .setCurrency( createPaymentDto.priceDetails().currency())
                .setPaymentMethod(createPaymentDto.paymentMethodId())
                .build();
    }
}
