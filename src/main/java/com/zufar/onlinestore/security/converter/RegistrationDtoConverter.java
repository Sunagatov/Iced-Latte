package com.zufar.onlinestore.security.converter;

import com.zufar.onlinestore.security.dto.UserRegistrationRequest;
import com.zufar.onlinestore.user.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RegistrationDtoConverter {

    UserDto toDto(final UserRegistrationRequest userRegistrationRequest);
}
