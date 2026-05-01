package com.zufar.icedlatte.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID userId) {
        super(String.format("User with id = %s is not found.", userId));
    }

    public UserNotFoundException(String email) {
        super(String.format("User with email = %s is not found.", email));
    }
}
