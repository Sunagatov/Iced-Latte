package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.openapi.dto.UserDto;
import com.zufar.onlinestore.user.converter.UserDtoConverter;
import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class SaveUserOperationPerformerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDtoConverter userDtoConverter;

    @Mock
    private DefaultUserEntityValuesSetter defaultUserEntityValuesSetter;

    @InjectMocks
    private SaveUserOperationPerformer saveUserOperationPerformer;

    @Test
    void shouldSaveUser() {
        UserDto user = new UserDto();
        user.setId(UUID.randomUUID());
        user.setFirstName("Username");
        user.setEmail("username@gmail.com");

        when(userRepository.save(any(UserEntity.class))).thenReturn(mock(UserEntity.class));
        doNothing().when(defaultUserEntityValuesSetter).setDefaultValues(any(UserEntity.class));
        when(userDtoConverter.toEntity(any(UserDto.class))).thenReturn(mock(UserEntity.class));
        when(userDtoConverter.toDto(any(UserEntity.class))).thenReturn(mock(UserDto.class));

        UserDto result = saveUserOperationPerformer.saveUser(user);

        assertNotNull(result);

        verify(userRepository, times(1)).save(any(UserEntity.class));
        verify(userDtoConverter, times(1)).toDto(any(UserEntity.class));
        verify(userDtoConverter, times(1)).toEntity(any(UserDto.class));
    }
}
