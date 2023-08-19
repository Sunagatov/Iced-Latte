package com.zufar.onlinestore.security.converter;

import com.zufar.onlinestore.security.dto.registration.UserRegistrationRequest;
import com.zufar.onlinestore.user.dto.UserDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RegistrationDtoConverter {

    UserDto toDto(final UserRegistrationRequest userRegistrationRequest);
}