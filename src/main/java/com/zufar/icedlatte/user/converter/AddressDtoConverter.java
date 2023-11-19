package com.zufar.icedlatte.user.converter;

import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.user.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AddressDtoConverter {

    @Named("toAddressDto")
    AddressDto toDto(final Address entity);

    @Named("toAddress")
    Address toEntity(final AddressDto dto);
}
