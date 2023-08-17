package com.zufar.onlinestore.payment.converter;

import com.stripe.param.CustomerCreateParams;
import com.zufar.onlinestore.user.dto.AddressDto;
import com.zufar.onlinestore.user.dto.UserDto;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

import static com.stripe.param.CustomerCreateParams.Address;

@Component
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StripeCustomerConverter {

    @Mapping(target = "address", expression = "java(toAddress(authorizedUser.address))")
    @Mapping(target = "name", expression = "java(toFullName(authorizedUser))")
    @Mapping(target = "metadata", expression = "java(toMetadata(authorizedUser.userId))")
    @Mapping(target = "email", source = "authorizedUser.email")
    CustomerCreateParams toStripeObject(UserDto authorizedUser);

    default Address toAddress(AddressDto address) {
        return Address.builder()
                .setCountry(address.country())
                .setCity(address.city())
                .setLine1(address.line())
                .build();
    }

    default Map<String, String> toMetadata(UUID authorizedUserId) {
        return Map.of("authorizedUserId", authorizedUserId.toString());
    }

    default String toFullName(UserDto authorizedUser) {
        return StringUtils.join(authorizedUser.firstName(), Character.SPACE_SEPARATOR, authorizedUser.lastName());
    }
}

