package com.zufar.onlinestore.security.converter;

import com.zufar.onlinestore.security.dto.registration.UserRegistrationRequest;
import com.zufar.onlinestore.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationDtoConverter {

    public UserDto toDto(final UserRegistrationRequest userRegistrationRequest) {
        return new UserDto(
                userRegistrationRequest.firstName(),
                userRegistrationRequest.lastName(),
                userRegistrationRequest.username(),
                userRegistrationRequest.email(),
                userRegistrationRequest.password(),
                null
        );
    }
}
