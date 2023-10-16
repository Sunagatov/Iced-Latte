package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.openapi.dto.UserDto;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class UserServiceTest {

    @Mock
    private SaveUserOperationPerformer saveUserOperationPerformer;

    @Mock
    private SingleUserProvider singleUserProvider;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldReturnUser() {
        UUID userId = UUID.randomUUID();

        when(singleUserProvider.getUserById(userId)).thenReturn(mock(UserDto.class));

        UserDto result = userService.getUserById(userId);

        assertNotNull(result);

        verify(singleUserProvider, times(1)).getUserById(userId);
    }

    @Test
    void shouldSaveUser() {
        UserDto user = new UserDto();
        user.setId(UUID.randomUUID());
        user.setFirstName("Username");
        user.setEmail("username@gmail.com");

        when(saveUserOperationPerformer.saveUser(user)).thenReturn(mock(UserDto.class));

        UserDto result = userService.saveUser(user);

        assertNotNull(result);

        verify(saveUserOperationPerformer, times(1)).saveUser(user);
    }
}
