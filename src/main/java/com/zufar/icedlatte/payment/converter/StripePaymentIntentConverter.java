package com.zufar.icedlatte.payment.converter;

import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentCreateParams;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.payment.calculator.PaymentPriceCalculator;
import com.zufar.icedlatte.payment.entity.Payment;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(uses = PaymentPriceCalculator.class, componentModel = MappingConstants.ComponentModel.SPRING)
public interface StripePaymentIntentConverter {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "paymentIntentId", source = "paymentIntent.id")
    @Mapping(target = "shoppingCartId", source = "shoppingCart.id")
    @Mapping(target = "itemsTotalPrice", source = "paymentIntent.amount", qualifiedByName = {"calculateForPayment"})
    Payment toEntity(final PaymentIntent paymentIntent,
                     final ShoppingCartDto shoppingCart);

    @Mapping(target = "customer", source = "paymentMethod.customer")
    @Mapping(target = "paymentMethod", source = "paymentMethod.id")
    @Mapping(target = "amount",
            source = "shoppingCart.itemsTotalPrice",
            qualifiedByName = {"calculateForPaymentIntent"})
    PaymentIntentCreateParams toStripeObject(final PaymentMethod paymentMethod,
                                             final ShoppingCartDto shoppingCart, final String currency);
}
