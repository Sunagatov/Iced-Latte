package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.user.converter.UserDtoConverter;
import com.zufar.onlinestore.openapi.dto.UserDto;
import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.exception.UserNotFoundException;
import com.zufar.onlinestore.user.repository.UserRepository;
import com.zufar.onlinestore.user.stub.UserDtoTestStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SingleUserProviderTest {

    @Mock
    private UserRepository userCrudRepository;

    @Mock
    private UserDtoConverter userDtoConverter;

    @InjectMocks
    private SingleUserProvider singleUserProvider;

    @Test
    @DisplayName("getUserById should return the correct UserDto when the user exists")
    public void getUserById_ShouldReturnCorrectUserDtoWhenUserExists() {
        UUID userId = UUID.fromString("ebd4d43f-3152-4af5-86dd-526a002cbbc3");
        UserEntity testUserEntity = UserDtoTestStub.createUserEntity();

        when(userCrudRepository.findById(userId)).thenReturn(java.util.Optional.of(testUserEntity));

        UserDto expectedUserDto = UserDtoTestStub.createUserDto();
        when(userDtoConverter.toDto(testUserEntity)).thenReturn(expectedUserDto);

        UserDto actualUserDto = singleUserProvider.getUserById(userId);

        assertEquals(expectedUserDto, actualUserDto);
        verify(userCrudRepository).findById(userId);
        verify(userDtoConverter).toDto(testUserEntity);
    }

    @Test
    @DisplayName("getUserById should throw UserNotFoundException when the user does not exist")
    public void getUserById_ShouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
        UUID nonExistentUserId = UUID.randomUUID();

        when(userCrudRepository.findById(nonExistentUserId)).thenReturn(java.util.Optional.empty());

        assertThrows(UserNotFoundException.class, () -> singleUserProvider.getUserById(nonExistentUserId));
        verify(userCrudRepository).findById(nonExistentUserId);
    }
}
