package com.zufar.onlinestore.payment.converter;

import com.stripe.model.Token;
import com.stripe.param.PaymentMethodCreateParams;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StripePaymentMethodConverter {

    @Mapping(target = "type", source = "retrievedCardDetailsToken.type")
    @Mapping(target = "card", source = "retrievedCardDetailsToken.card")
    PaymentMethodCreateParams toStripeObject(Token retrievedCardDetailsToken);
}
