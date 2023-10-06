package com.zufar.onlinestore.user.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class UserNotFoundException extends RuntimeException {

    private final UUID userId;
    private final String userEmail;

    public UserNotFoundException(UUID userId) {
        super(String.format("User with id = %s is not found.", userId));
        this.userId = userId;
        this.userEmail = null;
    }

    public UserNotFoundException(String userEmail) {
        super(String.format("User with email = %s is not found.", userEmail));
        this.userId = null;
        this.userEmail = userEmail;
    }
}
