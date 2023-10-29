package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.openapi.dto.UserDto;
import com.zufar.onlinestore.security.converter.RegistrationDtoConverter;
import com.zufar.onlinestore.security.dto.UserRegistrationRequest;
import com.zufar.onlinestore.security.jwt.JwtTokenProvider;
import com.zufar.onlinestore.user.api.UserApi;
import com.zufar.onlinestore.user.converter.UserDtoConverter;
import com.zufar.onlinestore.user.converter.UserDtoConverterImpl;
import com.zufar.onlinestore.user.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {
    @InjectMocks
    private UserRegistrationService userRegistrationService;
    @Mock
    private UserApi userApi;
    @Mock
    private RegistrationDtoConverter registrationDtoConverter;
    @Mock
    private UserDtoConverter userDtoConverter;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("mock test register")
    void testRegister() {
        UserRegistrationRequest request = new UserRegistrationRequest("name","lastName", "email", "password");
        UserDto userDto = new UserDto();
        UserDto userDtoWithId = new UserDto();
        UserEntity userDetails = new UserEntity();
        String jwtToken = "jwtToken";
        String password = request.password();

        Mockito.when(registrationDtoConverter.toDto(request))
                .thenReturn(userDto);
        Mockito.when(passwordEncoder.encode(request.password()))
                .thenReturn(password);
        Mockito.when(userApi.saveUser(userDto))
                .thenReturn(userDtoWithId);
        Mockito.when(userDtoConverter.toEntity(userDtoWithId))
                .thenReturn(userDetails);
        Mockito.when(jwtTokenProvider.generateToken(userDetails))
                .thenReturn(jwtToken);

        userRegistrationService.register(request);

        Mockito.verify(registrationDtoConverter, Mockito.times(1))
                .toDto(request);
        Mockito.verify(passwordEncoder, Mockito.times(1))
                .encode(request.password());
        Mockito.verify(userApi, Mockito.times(1))
                .saveUser(userDto);
        Mockito.verify(userDtoConverter, Mockito.times(1))
                .toEntity(userDtoWithId);
        Mockito.verify(jwtTokenProvider, Mockito.times(1))
                .generateToken(userDetails);
    }
}