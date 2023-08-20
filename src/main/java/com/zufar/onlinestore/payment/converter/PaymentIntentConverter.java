package com.zufar.onlinestore.payment.converter;

import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.zufar.onlinestore.payment.calculator.PaymentPriceCalculator;
import com.zufar.onlinestore.payment.dto.CreatePaymentDto;
import com.zufar.onlinestore.payment.entity.Payment;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = PaymentPriceCalculator.class)
public interface PaymentIntentConverter {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "paymentIntentId", source = "paymentIntent.id")
    @Mapping(target = "itemsTotalPrice", source = "paymentIntent.amount", qualifiedByName = {"calculateForPayment"})
    Payment toPayment(final PaymentIntent paymentIntent);

    @Mapping(target = "currency", constant = "usd")
    @Mapping(target = "paymentMethod", source = "createPaymentDto.paymentMethodId")
    @Mapping(target = "amount",
            source = "createPaymentDto.itemsTotalPrice",
            qualifiedByName = {"calculateForPaymentIntent"})
    PaymentIntentCreateParams toPaymentIntentParams(final CreatePaymentDto createPaymentDto);
}
