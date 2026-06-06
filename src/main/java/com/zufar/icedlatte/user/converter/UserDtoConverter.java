package com.zufar.icedlatte.user.converter;

import org.jspecify.annotations.Nullable;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.openapi.dto.UpdateUserAccountRequest;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.entity.Address;
import com.zufar.icedlatte.user.entity.UserEntity;

@SuppressWarnings("NullableProblems")
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = AddressDtoConverter.class,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserDtoConverter {

    @Mapping(target = "address", source = "address", qualifiedByName = "toAddressDto")
    @Mapping(target = "avatarLink", ignore = true)
    @Mapping(target = "oauthUser", source = "oauthUser")
    UserDto toDto(final UserEntity entity);

    @Mapping(target = "address", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "stripeCustomerToken", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    @Mapping(target = "accountNonExpired", ignore = true)
    @Mapping(target = "accountNonLocked", ignore = true)
    @Mapping(target = "credentialsNonExpired", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "oauthUser", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntity(@MappingTarget UserEntity entity, UpdateUserAccountRequest request);

    @AfterMapping
    default void updateAddress(@MappingTarget UserEntity entity, UpdateUserAccountRequest request) {
        AddressDto dto = request.getAddress();
        if (dto == null || isEmptyAddress(dto)) {
            entity.setAddress(null);
            return;
        }

        String country = required(dto.getCountry(), "country");
        String city = required(dto.getCity(), "city");
        String line = required(dto.getLine(), "line");
        String postcode = required(dto.getPostcode(), "postcode");

        if (entity.getAddress() == null) {
            entity.setAddress(Address.builder()
                    .country(country)
                    .city(city)
                    .line(line)
                    .postcode(postcode)
                    .build());
            return;
        }

        entity.getAddress().update(country, city, line, postcode);
    }

    private static boolean isEmptyAddress(AddressDto dto) {
        return isBlank(dto.getCountry())
                && isBlank(dto.getCity())
                && isBlank(dto.getLine())
                && isBlank(dto.getPostcode());
    }

    private static String required(@Nullable String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("address." + fieldName + " is required");
        }
        return value;
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }
}
