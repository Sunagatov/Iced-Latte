package com.zufar.icedlatte.payment.converter;

import com.zufar.icedlatte.openapi.dto.ShippingInfoDto;
import com.zufar.icedlatte.payment.entity.Shipping;
import com.zufar.icedlatte.user.converter.AddressDtoConverter;
import com.zufar.icedlatte.user.converter.UserDtoConverter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {UserDtoConverter.class, AddressDtoConverter.class})
public interface ShippingConverter {

    @Mapping(target = "user", source = "shippingInfoDto", qualifiedByName = "toUserEntity")
    @Mapping(target = "address", source = "shippingInfoDto.address", qualifiedByName = "toAddress")
    @Mapping(target = "name", source = "shippingInfoDto.shippingMethod")
    Shipping toEntity(ShippingInfoDto shippingInfoDto);

    @Mapping(target = "email", source = "shipping.user.email")
    @Mapping(target = "firstName", source = "shipping.user.firstName")
    @Mapping(target = "lastName", source = "shipping.user.lastName")
    @Mapping(target = "phoneNumber", source = "shipping.user.phoneNumber")
    @Mapping(target = "address", source = "shipping.address", qualifiedByName = "toAddressDto")
    @Mapping(target = "shippingMethod", source = "shipping.name")
    ShippingInfoDto toDto(Shipping shipping);

    List<ShippingInfoDto> toDtoList(List<Shipping> shippingList);

}
