package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.openapi.dto.UserDto;
import com.zufar.onlinestore.security.converter.RegistrationDtoConverter;
import com.zufar.onlinestore.security.dto.UserRegistrationRequest;
import com.zufar.onlinestore.security.dto.UserRegistrationResponse;
import com.zufar.onlinestore.security.jwt.JwtTokenProvider;
import com.zufar.onlinestore.user.api.UserApi;
import com.zufar.onlinestore.user.converter.UserDtoConverter;
import com.zufar.onlinestore.user.entity.UserEntity;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private UserRegistrationRequest request = Instancio.create(UserRegistrationRequest.class);
    private UserDto userDto = Instancio.create(UserDto.class);
    private String jwtToken = "TestJwtToken";
    private UserDto userDtoWithId = new UserDto();
    private UserEntity userDetails = new UserEntity();

    @Test
    @DisplayName("Register when user is successfully registered")
    public void registerWhenUserIsSuccessfullyRegistered() {
        userDto.setPassword(request.password());

        when(registrationDtoConverter.toDto(request)).thenReturn(userDto);
        when(passwordEncoder.encode(request.password())).thenReturn(request.password());
        when(userApi.saveUser(userDto)).thenReturn(userDtoWithId);
        when(userDtoConverter.toEntity(userDtoWithId)).thenReturn(userDetails);
        when(jwtTokenProvider.generateToken(userDetails)).thenReturn(jwtToken);

        UserRegistrationResponse response = userRegistrationService.register(request);

        assertEquals(response, new UserRegistrationResponse(jwtToken));
        verify(registrationDtoConverter, times(1)).toDto(request);
        verify(passwordEncoder, times(1)).encode(request.password());
        verify(userApi, times(1)).saveUser(userDto);
        verify(userDtoConverter, times(1)).toEntity(userDtoWithId);
        verify(jwtTokenProvider, times(1)).generateToken(userDetails);
    }
}