package com.zufar.onlinestore.payment.converter;

import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.zufar.onlinestore.payment.calculator.PaymentPriceCalculator;
import com.zufar.onlinestore.payment.dto.CreatePaymentDto;
import com.zufar.onlinestore.payment.entity.Payment;
import org.mapstruct.Mapper;

@Mapper
public interface PaymentIntentConverter {

    PaymentPriceCalculator paymentPriceCalculator = new PaymentPriceCalculator();

    default Payment toPayment(final PaymentIntent paymentIntent) {
        if (paymentIntent == null) {
            return null;
        }

        Payment.PaymentBuilder builder = Payment.builder();

        builder.itemsTotalPrice(paymentPriceCalculator.calculatePriceForPayment(paymentIntent.getAmount()));
        builder.paymentIntentId(paymentIntent.getId());
        builder.currency(paymentIntent.getCurrency());

        return builder.build();
    }

    default PaymentIntentCreateParams toPaymentIntentParams(final CreatePaymentDto createPaymentDto) {
        if (createPaymentDto == null) {
            return null;
        }

        PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder();

        builder.setAmount(paymentPriceCalculator.calculatePriceForPaymentIntent(
                createPaymentDto.priceDetails().itemsTotalPrice()));
        builder.setCurrency(createPaymentDto.priceDetails().currency());
        builder.setPaymentMethod(createPaymentDto.paymentMethodId());

        return builder.build();
    }
}
