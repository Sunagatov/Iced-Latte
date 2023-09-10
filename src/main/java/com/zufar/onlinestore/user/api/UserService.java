package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.user.converter.UserDtoConverter;
import com.zufar.onlinestore.user.dto.UserDto;
import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.exception.UserAlreadyRegisteredException;
import com.zufar.onlinestore.user.exception.UserNotFoundException;
import com.zufar.onlinestore.user.repository.UserRepository;
import com.zufar.onlinestore.user.validation.UserDataValidator;
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
    private final UserDataValidator userDataValidator;

    @Override
    public UserDto saveUser(final UserDto userDto) {
        UserEntity userEntity = userDtoConverter.toEntity(userDto);
        userDataValidator.validate(userEntity);
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

}
