package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.openapi.dto.UserDto;
import com.zufar.onlinestore.user.converter.UserDtoConverter;
import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.exception.UserNotFoundException;
import com.zufar.onlinestore.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class SingleUserProviderTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDtoConverter userDtoConverter;

    @InjectMocks
    private SingleUserProvider singleUserProvider;

    @Test
    void shouldReturnUserWhenUserIdExists() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(UserEntity.class)));
        when(userDtoConverter.toDto(any(UserEntity.class))).thenReturn(mock(UserDto.class));

        UserDto result = singleUserProvider.getUserById(userId);

        assertNotNull(result);

        verify(userDtoConverter, times(1)).toDto(any(UserEntity.class));
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenUserIdNotExists() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(userDtoConverter.toDto(any(UserEntity.class))).thenReturn(mock(UserDto.class));

        assertThrows(
                UserNotFoundException.class,
                () -> singleUserProvider.getUserById(userId)
        );

        verify(userRepository, times(1)).findById(userId);
    }
}
