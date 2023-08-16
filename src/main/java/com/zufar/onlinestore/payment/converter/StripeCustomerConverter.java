package com.zufar.onlinestore.payment.converter;

import com.stripe.param.CustomerCreateParams;
import com.zufar.onlinestore.user.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class StripeCustomerConverter {

    protected StripeAddressConverter stripeAddressConverter;

    @Mapping(target = "address", expression = "java(stripeAddressConverter.toStripeObject(authorizedUser.address()))")
    @Mapping(target = "name", expression = "java(String.join(authorizedUser.firstName(), String.valueOf(Character.SPACE_SEPARATOR)," +
            " authorizedUser.lastName()))")
    @Mapping(target = "metadata", expression = "java(java.util.Map.of(\"authorizedUserId\", authorizedUser.userId().toString()))")
    @Mapping(target = "email", source = "authorizedUser.email")
    public abstract CustomerCreateParams toStripeObject(UserDto authorizedUser);

    @Autowired
    public void setStripeAddressConverter(StripeAddressConverter stripeAddressConverter) {
        this.stripeAddressConverter = stripeAddressConverter;
    }
}

