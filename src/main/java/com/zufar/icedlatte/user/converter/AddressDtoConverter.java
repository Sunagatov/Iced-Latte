package com.zufar.icedlatte.user.converter;

import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.user.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AddressDtoConverter {

    @Named("toAddressDto")
    AddressDto toDto(final Address entity);

    @Named("toAddress")
    Address toEntity(final AddressDto dto);
}
