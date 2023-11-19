package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.security.converter.RegistrationDtoConverter;
import com.zufar.icedlatte.security.dto.UserRegistrationRequest;
import com.zufar.icedlatte.user.converter.UserDtoConverter;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.repository.UserRepository;
import com.zufar.icedlatte.user.stub.UserDtoTestStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaveUserOperationPerformerTest {

    @Mock
    private UserRepository userCrudRepository;

    @Mock
    private UserDtoConverter userDtoConverter;

    @Mock
    private RegistrationDtoConverter registrationDtoConverter;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DefaultUserEntityValuesSetter defaultUserEntityValuesSetter;

    @InjectMocks
    private SaveUserOperationPerformer saveUserOperationPerformer;

    @Test
    @DisplayName("saveUser should save the user and return the corresponding UserDto")
    void saveUser_ShouldSaveUserAndReturnUserDto() {
        UserDto userDto = UserDtoTestStub.createUserDto();
        UserRegistrationRequest userRegistrationRequest = new UserRegistrationRequest("John", "Doe", "john.doe@example.com", "password123");
        UserEntity userEntity = UserDtoTestStub.createUserEntity();
        String encodedPassword = "encodedPassword123";

        when(registrationDtoConverter.toEntity(userRegistrationRequest)).thenReturn(userEntity);
        when(passwordEncoder.encode(userRegistrationRequest.password())).thenReturn(encodedPassword);
        when(userCrudRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        when(userDtoConverter.toDto(userEntity)).thenReturn(userDto);

        UserDto actualUserDto = saveUserOperationPerformer.saveUser(userRegistrationRequest);

        assertEquals(userDto, actualUserDto);
        verify(registrationDtoConverter, times(1)).toEntity(userRegistrationRequest);
        verify(passwordEncoder, times(1)).encode(userRegistrationRequest.password());
        verify(defaultUserEntityValuesSetter, times(1)).setDefaultValues(userEntity);
        verify(userCrudRepository, times(1)).save(userEntity);
        verify(userDtoConverter, times(1)).toDto(userEntity);

        assertEquals(encodedPassword, userEntity.getPassword());
    }
}
