package com.zufar.icedlatte.user.converter;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.mapstruct.*;

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
        Optional<UserAddressParts> maybeAddressParts = readAddressParts(dto);
        if (maybeAddressParts.isEmpty()) {
            entity.setAddress(null);
            return;
        }
        UserAddressParts addressParts = maybeAddressParts.orElseThrow();

        if (entity.getAddress() == null) {
            entity.setAddress(Address.builder()
                    .country(addressParts.country())
                    .city(addressParts.city())
                    .line(addressParts.line())
                    .postcode(addressParts.postcode())
                    .build());
            return;
        }

        entity.getAddress()
                .update(addressParts.country(), addressParts.city(), addressParts.line(), addressParts.postcode());
    }

    private static Optional<UserAddressParts> readAddressParts(@Nullable AddressDto dto) {
        if (dto == null || isBlankAddress(dto)) {
            return Optional.empty();
        }
        return Optional.of(new UserAddressParts(
                requireAddressPart(dto.getCountry(), "country"),
                requireAddressPart(dto.getCity(), "city"),
                requireAddressPart(dto.getLine(), "line"),
                requireAddressPart(dto.getPostcode(), "postcode")));
    }

    private static boolean isBlankAddress(AddressDto dto) {
        return isBlank(dto.getCountry())
                && isBlank(dto.getCity())
                && isBlank(dto.getLine())
                && isBlank(dto.getPostcode());
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }

    private static String requireAddressPart(@Nullable String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("address." + fieldName + " is required");
        }
        return value;
    }
}

record UserAddressParts(String country, String city, String line, String postcode) {}
