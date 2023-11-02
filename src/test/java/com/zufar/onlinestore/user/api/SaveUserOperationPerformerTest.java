package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.openapi.dto.UserDto;
import com.zufar.onlinestore.user.converter.UserDtoConverter;
import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.repository.UserRepository;
import com.zufar.onlinestore.user.stub.UserDtoTestStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SaveUserOperationPerformerTest {

    @Mock
    private UserRepository userCrudRepository;

    @Mock
    private UserDtoConverter userDtoConverter;

    @Mock
    private DefaultUserEntityValuesSetter defaultUserEntityValuesSetter;

    @InjectMocks
    private SaveUserOperationPerformer saveUserOperationPerformer;

    @Test
    @DisplayName("saveUser should save the user and return the corresponding UserDto")
    public void saveUser_ShouldSaveUserAndReturnUserDto() {
        UserDto userDto = UserDtoTestStub.createUserDto();
        UserEntity userEntity = UserDtoTestStub.createUserEntity();

        when(userDtoConverter.toEntity(userDto)).thenReturn(userEntity);
        when(userCrudRepository.save(userEntity)).thenReturn(userEntity);
        UserDto expectedUserDto = UserDtoTestStub.createUserDto();
        when(userDtoConverter.toDto(userEntity)).thenReturn(expectedUserDto);

        UserDto actualUserDto = saveUserOperationPerformer.saveUser(userDto);

        assertEquals(expectedUserDto, actualUserDto);
        verify(userDtoConverter).toEntity(userDto);
        verify(defaultUserEntityValuesSetter).setDefaultValues(userEntity);
        verify(userCrudRepository).save(userEntity);
        verify(userDtoConverter).toDto(userEntity);
    }
}
