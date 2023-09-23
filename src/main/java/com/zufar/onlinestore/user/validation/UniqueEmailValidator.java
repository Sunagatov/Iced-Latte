package com.zufar.onlinestore.user.validation;

import com.zufar.onlinestore.user.repository.UserRepository;
import com.zufar.onlinestore.user.validation.annotation.UniqueEmail;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Component
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    private final UserRepository userCrudRepository;

    @Override
    public boolean isValid(String email, ConstraintValidatorContext constraintValidatorContext) {
        return userCrudRepository
                .findByEmail(email)
                .isPresent();
    }
}
