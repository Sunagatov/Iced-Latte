package com.zufar.icedlatte.payment.converter;

import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentCreateParams;
import com.zufar.icedlatte.openapi.dto.ShippingInfoDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.payment.calculator.PaymentPriceCalculator;
import com.zufar.icedlatte.payment.entity.Payment;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;

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
                                             final ShoppingCartDto shoppingCart,
                                             final ShippingInfoDto shippingInfo,
                                             final String currency);
    @AfterMapping
    default void setShippingDetails(@MappingTarget PaymentIntentCreateParams.Builder builder,
                                    final ShippingInfoDto shippingInfo) {
        PaymentIntentCreateParams.Shipping shippingDetails = PaymentIntentCreateParams.Shipping.builder()
                .setName(toFullName(shippingInfo.getShippingUserFirstName(), shippingInfo.getShippingUserLastName()))
                .setPhone(shippingInfo.getShippingUserPhoneNumber())
                .setAddress(
                        PaymentIntentCreateParams.Shipping.Address.builder()
                                .setLine1(shippingInfo.getShippingAddress().getAddressLine())
                                .setCity(shippingInfo.getShippingAddress().getCity())
                                .setCountry(shippingInfo.getShippingAddress().getCountry())
                                .setPostalCode(shippingInfo.getShippingAddress().getPostCode())
                                .build()
                )
                .build();

        builder.setShipping(shippingDetails);
        builder.putMetadata("shippingUserEmail", shippingInfo.getShippingUserEmail());
        builder.putMetadata("shippingMethod", shippingInfo.getShippingMethod());
    }

    private String toFullName(String firstName, String lastName) {
        return StringUtils.join(firstName, Character.SPACE_SEPARATOR, lastName);
    }
}
