package com.zufar.onlinestore.user.exception;

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

    public UserNotFoundException(String userName) {
        super(String.format("User with userName = %s is not found.", userName));
        this.userId = null;
        this.userName = userName;
    }
}
