package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.user.converter.UserDtoConverter;
import com.zufar.onlinestore.user.dto.UserDto;
import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.exception.UserAlreadyRegisteredException;
import com.zufar.onlinestore.user.exception.UserNotFoundException;
import com.zufar.onlinestore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserApi {

    private final UserRepository userCrudRepository;
    private final UserDtoConverter userDtoConverter;
    private final AuthorityService authorityService;

    @Override
    public UserDto saveUser(final UserDto userDto) {
        UserEntity userEntity = userDtoConverter.toEntity(userDto);

        List<String> errors = new ArrayList<>();

        if (!isEmailUnique(userEntity.getUserId(), userEntity.getEmail())) {
            errors.add(String.format("User with email = %s is already registered", userEntity.getEmail()));
        }

        if (!isUsernameUnique(userEntity.getUserId(), userEntity.getUsername())) {
            errors.add(String.format("User with username = %s is already registered", userEntity.getUsername()));
        }

        if (!errors.isEmpty()) {

            throw new UserAlreadyRegisteredException(errors);

        }

        UserEntity userEntityWithId = userCrudRepository.save(userEntity);
        authorityService.setDefaultAuthority(userEntityWithId);

        return userDtoConverter.toDto(userEntityWithId);
    }

    @Override
    public UserDto getUserById(final UUID userId) throws UserNotFoundException {
        Optional<UserEntity> userEntity = userCrudRepository.findById(userId);
        if (userEntity.isEmpty()) {
            log.warn("Failed to get the user with the userId = {}.", userId);
            throw new UserNotFoundException(userId);
        }
        return userDtoConverter.toDto(userEntity.get());
    }

    public boolean isEmailUnique(UUID id, String email) {
        Optional<UserEntity> userByEmail = userCrudRepository.findByEmail(email);

        if (userByEmail.isEmpty()) {
            return true;
        }

        return id == null || userByEmail.map(UserEntity::getUserId).filter(userId -> userId == id).isPresent();
    }

    public boolean isUsernameUnique(UUID id, String username) {
        UserEntity userByUsername = userCrudRepository.findUserByUsername(username);

        if (userByUsername == null) {
            return true;
        }

        if (id == null) {
            return false;
        } else {
            return userByUsername.getUserId() == id;
        }
    }
}
