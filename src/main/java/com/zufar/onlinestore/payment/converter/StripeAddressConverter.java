package com.zufar.onlinestore.payment.converter;

import com.stripe.param.CustomerCreateParams;
import com.zufar.onlinestore.user.dto.AddressDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StripeAddressConverter {

    @Named("toStripeObject")
    @Mapping(target = "city", source = "address.city")
    @Mapping(target = "country", source = "address.country")
    @Mapping(target = "line1", source = "address.line")
    CustomerCreateParams.Address toStripeObject(AddressDto address);

}
