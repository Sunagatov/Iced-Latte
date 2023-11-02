package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.openapi.dto.UserDto;
import com.zufar.onlinestore.user.exception.UserNotFoundException;
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
public class UserServiceTest {

    @Mock
    private SaveUserOperationPerformer saveUserOperationPerformer;

    @Mock
    private SingleUserProvider singleUserProvider;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("saveUser should save the user and return the corresponding UserDto")
    public void saveUser_ShouldSaveUserAndReturnUserDto() {
        UserDto userDto = UserDtoTestStub.createUserDto();
        UserDto expectedUserDto = UserDtoTestStub.createUserDto();

        when(saveUserOperationPerformer.saveUser(userDto)).thenReturn(expectedUserDto);

        UserDto actualUserDto = userService.saveUser(userDto);

        assertEquals(expectedUserDto, actualUserDto);
        verify(saveUserOperationPerformer).saveUser(userDto);
    }

    @Test
    @DisplayName("getUserById should return the correct UserDto when the user exists")
    public void getUserById_ShouldReturnCorrectUserDtoWhenUserExists() throws UserNotFoundException {
        UUID userId = UUID.fromString("ebd4d43f-3152-4af5-86dd-526a002cbbc3");
        UserDto expectedUserDto = UserDtoTestStub.createUserDto();

        when(singleUserProvider.getUserById(userId)).thenReturn(expectedUserDto);

        UserDto actualUserDto = userService.getUserById(userId);

        assertEquals(expectedUserDto, actualUserDto);
        verify(singleUserProvider).getUserById(userId);
    }

    @Test
    @DisplayName("getUserById should throw UserNotFoundException when the user does not exist")
    public void getUserById_ShouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
        UUID nonExistentUserId = UUID.randomUUID();

        when(singleUserProvider.getUserById(nonExistentUserId)).thenThrow(UserNotFoundException.class);

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(nonExistentUserId));
        verify(singleUserProvider).getUserById(nonExistentUserId);
    }
}
