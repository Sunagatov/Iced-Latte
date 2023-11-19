package com.zufar.icedlatte.user.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class UserNotFoundException extends RuntimeException {

    private final UUID userId;
    private final String userName;

    public UserNotFoundException(UUID userId) {
        super(String.format("User with id = %s is not found.", userId));
        this.userId = userId;
        this.userName = null;
    }
}
