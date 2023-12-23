package com.zufar.icedlatte.payment.converter;

import com.zufar.icedlatte.openapi.dto.ShippingInfoDto;
import com.zufar.icedlatte.payment.entity.Shipping;
import com.zufar.icedlatte.payment.entity.ShippingAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ShippingConverter {

    default Shipping toEntity(ShippingInfoDto dto) {
        if (dto == null) {
            return null;
        }
        var shipping = new Shipping();
        shipping.setShippingUserEmail(dto.getShippingUserEmail());
        shipping.setShippingUserFirstName(dto.getShippingUserFirstName());
        shipping.setShippingUserLastName(dto.getShippingUserLastName());
        shipping.setShippingUserPhoneNumber(dto.getShippingUserPhoneNumber());
        shipping.setShippingMethod(dto.getShippingMethod());

        var shippingAddress = new ShippingAddress();
        if (dto.getAddress() != null) {
            shippingAddress.setCountry(dto.getAddress().getCountry());
            shippingAddress.setAddressLine(dto.getAddress().getAddressLine());
            shippingAddress.setCity(dto.getAddress().getCity());
            shippingAddress.setZipCode(dto.getAddress().getZipCode());
        }
        shipping.setShippingAddress(shippingAddress);
        return shipping;
    }

    @Mappings({
            @Mapping(target = "shippingUserEmail", source = "shipping.shippingUserEmail"),
            @Mapping(target = "shippingUserFirstName", source = "shipping.shippingUserFirstName"),
            @Mapping(target = "shippingUserLastName", source = "shipping.shippingUserLastName"),
            @Mapping(target = "shippingUserPhoneNumber", source = "shipping.shippingUserPhoneNumber"),
            @Mapping(target = "shippingMethod", source = "shipping.shippingMethod"),
            @Mapping(target = "address.country", source = "shipping.shippingAddress.country"),
            @Mapping(target = "address.addressLine", source = "shipping.shippingAddress.addressLine"),
            @Mapping(target = "address.city", source = "shipping.shippingAddress.city"),
            @Mapping(target = "address.zipCode", source = "shipping.shippingAddress.zipCode"),
    })
    ShippingInfoDto toDto(Shipping shipping);
    List<ShippingInfoDto> toDtoList(List<Shipping> shippingList);
}
