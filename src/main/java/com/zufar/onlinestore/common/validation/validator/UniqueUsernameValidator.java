package com.zufar.onlinestore.common.validation.validator;

import com.zufar.onlinestore.common.validation.annotation.UniqueUsername;
import com.zufar.onlinestore.user.repository.UserRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UniqueUsernameValidator implements ConstraintValidator<UniqueUsername, String> {

    private final UserRepository userCrudRepository;

    @Override
    public boolean isValid(String username, ConstraintValidatorContext constraintValidatorContext) {
        return userCrudRepository.findUserByUsername(username) == null;
    }
}
