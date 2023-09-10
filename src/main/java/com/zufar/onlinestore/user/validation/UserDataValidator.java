package com.zufar.onlinestore.user.validation;

import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.exception.UserAlreadyRegisteredException;
import com.zufar.onlinestore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class UserDataValidator {

    private final List<String> errors;
    private final UserRepository userCrudRepository;

    public void validate(UserEntity user) {
        if (!isEmailUnique(user.getUserId(), user.getEmail())) {
            errors.add(String.format("User with email = %s is already registered", user.getEmail()));
        }

        if (!isUsernameUnique(user.getUserId(), user.getUsername())) {
            errors.add(String.format("User with username = %s is already registered", user.getUsername()));
        }

        if (!errors.isEmpty()) {

            throw new UserAlreadyRegisteredException(errors);

        }
    }

    public boolean isEmailUnique(UUID id, String email) {
        Optional<UserEntity> userByEmail = userCrudRepository.findByEmail(email);

        if (userByEmail.isEmpty()) {
            return true;
        }

        return userByEmail.map(UserEntity::getUserId).filter(userId -> userId == id).isPresent();
    }

    public boolean isUsernameUnique(UUID id, String username) {
        UserEntity userByUsername = userCrudRepository.findUserByUsername(username);

        if (userByUsername == null) {
            return true;
        }

        return userByUsername.getUserId().equals(id);
    }
}

