package com.zufar.onlinestore.user.converter;

import com.zufar.onlinestore.openapi.dto.UserDto;
import com.zufar.onlinestore.user.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = AddressDtoConverter.class)
public interface UserDtoConverter {

    @Mapping(target = "address", source = "address", qualifiedByName = "toAddressDto")
    UserDto toDto(final UserEntity entity);

    @Mapping(target = "address", source = "address", qualifiedByName = "toAddress")
    UserEntity toEntity(final UserDto dto);
}
