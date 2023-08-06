package com.zufar.onlinestore.payment.converter;

import com.stripe.param.CustomerCreateParams;
import com.zufar.onlinestore.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Mapper(componentModel = "spring")
public abstract class StripeCustomerConverter {

    private final StripeAddressConverter stripeAddressConverter;

    @Mapping(target = "address", expression = "java(stripeAddressConverter.toStripeObject(user.address()))")
    @Mapping(target = "name", expression = "java(String.join(user.firstName(), String.valueOf(Character.SPACE_SEPARATOR), user.lastName()))")
    @Mapping(target = "email", source = "user.email")
    public abstract CustomerCreateParams toStripeObject(UserDto user);

}

