package com.zufar.icedlatte.user.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID userId) {
        super(String.format("User with id = %s is not found.", userId));
    }
}
