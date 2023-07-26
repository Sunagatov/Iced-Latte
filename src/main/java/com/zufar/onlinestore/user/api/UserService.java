package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.user.converter.UserDtoConverter;
import com.zufar.onlinestore.user.dto.UserDto;
import com.zufar.onlinestore.user.entity.User;
import com.zufar.onlinestore.user.exception.UserNotFoundException;
import com.zufar.onlinestore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserApi {

    private final UserRepository userCrudRepository;
    private final UserDtoConverter userDtoConverter;

    @Override
    public UserDto saveUser(UserDto saveUserRequest) {
        User userEntity = userDtoConverter.toEntity(saveUserRequest);
        User userEntityWithId = userCrudRepository.save(userEntity);
        return userDtoConverter.toDto(userEntityWithId);
    }

    @Override
    public UserDto getUserById(UUID userId) throws UserNotFoundException {
        Optional<User> user = userCrudRepository.findById(userId);
        if (user.isEmpty()) {
            log.warn("Failed to get the user with the id = {}.", userId);
            throw new UserNotFoundException(userId);
        }
        return userDtoConverter.toDto(user.get());
    }
}
