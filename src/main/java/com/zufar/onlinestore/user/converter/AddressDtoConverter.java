package com.zufar.onlinestore.user.converter;

import com.zufar.onlinestore.openapi.dto.AddressDto;
import com.zufar.onlinestore.user.entity.Address;
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
