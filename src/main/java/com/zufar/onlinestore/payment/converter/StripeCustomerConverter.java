package com.zufar.onlinestore.payment.converter;

import com.stripe.param.CustomerCreateParams;
import com.zufar.onlinestore.user.entity.Address;
import com.zufar.onlinestore.user.entity.UserEntity;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StripeCustomerConverter {

    @Mapping(target = "putAllExtraParam", ignore = true)
    @Mapping(target = "cashBalance", ignore = true)
    @Mapping(target = "invoiceSettings", ignore = true)
    @Mapping(target = "putAllMetadata", ignore = true)
    @Mapping(target = "tax", ignore = true)
    @Mapping(target = "address", expression = "java(toAddress(authorizedUser.getAddress()))")
    @Mapping(target = "name", expression = "java(toFullName(authorizedUser))")
    @Mapping(target = "metadata", source = "metadata")
    @Mapping(target = "email", source = "authorizedUser.email")
    CustomerCreateParams toStripeObject(UserEntity authorizedUser, Map<String, String> metadata);

    default CustomerCreateParams.Address toAddress(Address address) {
        CustomerCreateParams.Address stripeAddress = null;
        if (address != null) {
            stripeAddress = CustomerCreateParams.Address.builder()
                    .setCountry(address.getCountry())
                    .setCity(address.getCity())
                    .setLine1(address.getLine())
                    .build();
        }
        return stripeAddress;
    }

    default String toFullName(UserEntity authorizedUser) {
        return StringUtils.join(authorizedUser.getFirstName(), Character.SPACE_SEPARATOR, authorizedUser.getLastName());
    }
}

