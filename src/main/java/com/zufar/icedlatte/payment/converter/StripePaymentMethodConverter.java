package com.zufar.icedlatte.payment.converter;

import com.stripe.model.Token;
import com.stripe.param.PaymentMethodCreateParams;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StripePaymentMethodConverter {

    @Mapping(target = "type", source = "token", qualifiedByName = {"toType"})
    @Mapping(target = "card", source = "token.id", qualifiedByName = {"toToken"})
    PaymentMethodCreateParams toStripeObject(Token token);

    @Named("toToken")
    default PaymentMethodCreateParams.Token toToken(String tokenId) {
        return PaymentMethodCreateParams.Token.builder()
                .setToken(tokenId)
                .build();
    }

    @Named("toType")
    default PaymentMethodCreateParams.Type toType(Token token) {
        return PaymentMethodCreateParams.Type.valueOf(token.getType().toUpperCase());
    }
}
