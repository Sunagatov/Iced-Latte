package com.zufar.onlinestore.security.converter;

import com.zufar.onlinestore.security.dto.authentication.RegistrationRequest;
import com.zufar.onlinestore.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationDtoConverter {

    public UserDto toDto(final RegistrationRequest registrationRequest) {
        return new UserDto(
                null,
                registrationRequest.firstName(),
                registrationRequest.lastName(),
                registrationRequest.username(),
                registrationRequest.email(),
                registrationRequest.password(),
                registrationRequest.address()
        );
    }
}
