package com.zufar.icedlatte.user.converter;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.user.entity.Address;

@SuppressWarnings("NullableProblems")
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AddressDtoConverter {

    @Named("toAddressDto")
    AddressDto toDto(final Address entity);

    @Named("toAddress")
    default @Nullable Address toEntity(final @Nullable AddressDto dto) {
        if (dto == null) {
            return null;
        }
        if (isBlankAddress(dto)) {
            return null;
        }
        return Address.builder()
                .country(dto.getCountry())
                .city(dto.getCity())
                .line(dto.getLine())
                .postcode(dto.getPostcode())
                .build();
    }

    static boolean isBlankAddress(@Nullable AddressDto dto) {
        return dto == null
                || isBlank(dto.getCountry())
                        && isBlank(dto.getCity())
                        && isBlank(dto.getLine())
                        && isBlank(dto.getPostcode());
    }

    static String requireAddressPart(@Nullable String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("address." + fieldName + " is required");
        }
        return value;
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
