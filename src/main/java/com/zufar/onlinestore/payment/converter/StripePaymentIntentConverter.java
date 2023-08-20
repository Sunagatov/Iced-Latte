package com.zufar.onlinestore.payment.converter;

import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentCreateParams;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.payment.calculator.PaymentPriceCalculator;
import com.zufar.onlinestore.payment.entity.Payment;;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(uses = PaymentPriceCalculator.class, componentModel = MappingConstants.ComponentModel.SPRING)
public interface StripePaymentIntentConverter {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "paymentIntentId", source = "paymentIntent.id")
    @Mapping(target = "shoppingSessionId", source = "shoppingSession.id")
    @Mapping(target = "itemsTotalPrice", source = "paymentIntent.amount", qualifiedByName = {"calculateForPayment"})
    Payment toEntity(final PaymentIntent paymentIntent, final ShoppingSessionDto shoppingSession);

    @Mapping(target = "customer", source = "paymentMethod.customer")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "paymentMethod", source = "paymentMethod.id")
    @Mapping(target = "amount",
            source = "shoppingSession.itemsTotalPrice",
            qualifiedByName = {"calculateForPaymentIntent"})
    PaymentIntentCreateParams toStripeObject(final PaymentMethod paymentMethod, final ShoppingSessionDto shoppingSession, final String currency);
}
