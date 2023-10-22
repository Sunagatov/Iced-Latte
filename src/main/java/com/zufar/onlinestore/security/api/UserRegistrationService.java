package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.security.converter.RegistrationDtoConverter;
import com.zufar.onlinestore.security.dto.UserRegistrationRequest;
import com.zufar.onlinestore.security.dto.UserRegistrationResponse;
import com.zufar.onlinestore.security.jwt.JwtTokenProvider;
import com.zufar.onlinestore.user.api.UserApi;
import com.zufar.onlinestore.user.converter.UserDtoConverter;
import com.zufar.onlinestore.openapi.dto.UserDto;
import com.zufar.onlinestore.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserApi userApi;
    private final RegistrationDtoConverter registrationDtoConverter;
    private final UserDtoConverter userDtoConverter;
    private final JwtTokenProvider jwtTokenProvider;

    public UserRegistrationResponse register(final UserRegistrationRequest request) {
        log.info("Received registration request from {}.", request.email());
        final UserDto userDto = registrationDtoConverter.toDto(request);
        final UserDto userDtoWithId = userApi.saveUser(userDto);
        UserEntity userDetails = userDtoConverter.toEntity(userDtoWithId);
        final String jwtToken = jwtTokenProvider.generateToken(userDetails);
        log.info("Registration was successful for {}.", request.email());
        return new UserRegistrationResponse(jwtToken);
    }
}
