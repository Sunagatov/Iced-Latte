package com.zufar.icedlatte.user.converter;

import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.user.entity.Address;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AddressDtoConverter {

    @Named("toAddressDto")
    AddressDto toDto(final Address entity);

    @Named("toAddress")
    default Address toEntity(final AddressDto dto) {
        if (dto == null) {
            return null;
        }
        boolean allBlank = StringUtils.isBlank(dto.getCountry())
                && StringUtils.isBlank(dto.getCity())
                && StringUtils.isBlank(dto.getLine())
                && StringUtils.isBlank(dto.getPostcode());
        if (allBlank) {
            return null;
        }
        return Address.builder()
                .country(dto.getCountry())
                .city(dto.getCity())
                .line(dto.getLine())
                .postcode(dto.getPostcode())
                .build();
    }
}
