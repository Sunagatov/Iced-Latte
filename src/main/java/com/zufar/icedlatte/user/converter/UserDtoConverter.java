package com.zufar.icedlatte.user.converter;

import org.mapstruct.*;

import com.zufar.icedlatte.openapi.dto.UpdateUserAccountRequest;
import com.zufar.icedlatte.openapi.dto.UserDto;
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

    @Mapping(target = "address", source = "address", qualifiedByName = "toAddress")
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
}
