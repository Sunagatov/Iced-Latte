package com.zufar.onlinestore.security.converter;

import com.zufar.onlinestore.security.dto.registration.UserRegistrationRequest;
import com.zufar.onlinestore.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationDtoConverter {

    public UserDto toDto(final UserRegistrationRequest userRegistrationRequest) {
        return UserDto.builder()
                .firstName(userRegistrationRequest.firstName())
                .lastName(userRegistrationRequest.lastName())
                .username(userRegistrationRequest.username())
                .email(userRegistrationRequest.email())
                .password(userRegistrationRequest.password())
                .build();
    }
}
