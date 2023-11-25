package com.zufar.icedlatte.user.converter;

import com.zufar.icedlatte.openapi.dto.UpdateUserAccountRequest;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = AddressDtoConverter.class)
public interface UserDtoConverter {

    @Mapping(target = "address", source = "entity.address", qualifiedByName = "toAddressDto")
    UserDto toDto(final UserEntity entity);

    @Mapping(target = "address", source = "updateUserAccountRequest.address", qualifiedByName = "toAddress")
    UserEntity toEntity(final UpdateUserAccountRequest updateUserAccountRequest);
}
