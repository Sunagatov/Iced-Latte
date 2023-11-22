package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.security.converter.RegistrationDtoConverter;
import com.zufar.icedlatte.security.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.dto.UserRegistrationResponse;
import com.zufar.icedlatte.security.jwt.JwtTokenProvider;
import com.zufar.icedlatte.user.api.UserApi;
import com.zufar.icedlatte.user.converter.UserDtoConverter;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.stub.UserDtoTestStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRegistrationService Tests")
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

    private final UserRegistrationRequest request = new UserRegistrationRequest("John", "Doe", "john.doe@example.com", "password123");
    private final UserDto userDtoWithId = new UserDto();
    private final UserEntity userDetails = UserDtoTestStub.createUserEntity();

    @Test
    @DisplayName("Should Successfully Register User And Return JWT Token")
    void shouldSuccessfullyRegisterUserAndReturnJwtToken() {
        String jwtToken = "TestJwtToken";

        when(userApi.saveUser(request)).thenReturn(userDtoWithId);
        when(userDtoConverter.toEntity(userDtoWithId)).thenReturn(userDetails);
        when(jwtTokenProvider.generateToken(userDetails)).thenReturn(jwtToken);

        UserRegistrationResponse response = userRegistrationService.register(request);

        assertEquals(new UserRegistrationResponse(jwtToken), response);
        verify(userApi, times(1)).saveUser(request);
        verify(userDtoConverter, times(1)).toEntity(userDtoWithId);
        verify(jwtTokenProvider, times(1)).generateToken(userDetails);
    }


    @Test
    @DisplayName("Should Handle Token Generation Failure During Registration")
    void shouldHandleTokenGenerationFailureDuringRegistration() {
        when(userApi.saveUser(request)).thenReturn(userDtoWithId);
        when(userDtoConverter.toEntity(userDtoWithId)).thenReturn(userDetails);
        when(jwtTokenProvider.generateToken(userDetails)).thenThrow(new RuntimeException("Token generation failed"));

        Exception exception = assertThrows(RuntimeException.class, () -> userRegistrationService.register(request));

        assertEquals("Token generation failed", exception.getMessage());
        verify(userApi, times(1)).saveUser(request);
        verify(userDtoConverter, times(1)).toEntity(userDtoWithId);
        verify(jwtTokenProvider, times(1)).generateToken(userDetails);
    }
}
