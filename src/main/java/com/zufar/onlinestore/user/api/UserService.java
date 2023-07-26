package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.user.converter.UserDtoConverter;
import com.zufar.onlinestore.user.dto.UserDto;
import com.zufar.onlinestore.user.entity.UserEntity;
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
    public UserDto saveUser(final UserDto saveUserRequest) {
        UserEntity userEntity = userDtoConverter.toEntity(saveUserRequest);
        UserEntity userEntityWithId = userCrudRepository.save(userEntity);
        return userDtoConverter.toDto(userEntityWithId);
    }

    @Override
    public UserDto getUserById(final UUID userId) throws UserNotFoundException {
        Optional<UserEntity> user = userCrudRepository.findById(userId);
        if (user.isEmpty()) {
            log.warn("Failed to get the user with the userId = {}.", userId);
            throw new UserNotFoundException(userId);
        }
        return userDtoConverter.toDto(user.get());
    }

    @Override
    public UserDto getUserByUserName(final String userName) throws UserNotFoundException {
        UserEntity user = userCrudRepository.findUserByUserName(userName);
        if (user == null) {
            log.warn("Failed to get the user with the userName = {}.", userName);
            throw new UserNotFoundException(userName);
        }
        return userDtoConverter.toDto(user);
    }


}
