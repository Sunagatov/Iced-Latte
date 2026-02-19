package com.zufar.icedlatte.user.converter;

import com.zufar.icedlatte.openapi.dto.UpdateUserAccountRequest;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = AddressDtoConverter.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserDtoConverter {

    @Mapping(target = "address", source = "address", qualifiedByName = "toAddressDto")
    @Mapping(target = "avatarLink", ignore = true)
    UserDto toDto(final UserEntity entity);

    @Mapping(target = "address", source = "address", qualifiedByName = "toAddress")
    UserEntity toEntity(final UpdateUserAccountRequest updateUserAccountRequest);
}
