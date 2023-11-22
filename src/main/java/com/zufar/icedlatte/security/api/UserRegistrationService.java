package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.security.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.dto.UserRegistrationResponse;
import com.zufar.icedlatte.security.jwt.JwtTokenProvider;
import com.zufar.icedlatte.user.api.UserApi;
import com.zufar.icedlatte.user.converter.UserDtoConverter;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserApi userApi;
    private final UserDtoConverter userDtoConverter;
    private final JwtTokenProvider jwtTokenProvider;

    public UserRegistrationResponse register(final UserRegistrationRequest userRegistrationRequest) {
        log.info("Received registration request from {}.", userRegistrationRequest.email());
        final UserDto userDto = registrationDtoConverter.toDto(userRegistrationRequest);
        String encodedPassword = passwordEncoder.encode(userDto.getPassword());
        userDto.setPassword(encodedPassword);
        final UserDto userDtoWithId = userApi.saveUser(userDto);
        userApi.sendEmailConfirmationToken(userDtoWithId.getId());
        UserEntity userDetails = userDtoConverter.toEntity(userDtoWithId);
        final String jwtToken = jwtTokenProvider.generateToken(userDetails);
        log.info("Registration was successful for {}.", userDtoWithId.getEmail());
        return new UserRegistrationResponse(jwtToken);
    }

    public void confirmRegistrationEmail(final String token) {
        log.info("Received email confirmation request for token {}.", token);
        userApi.confirmUserEmail(token);
        log.info("Token {} was validated.", token);
    }
}
