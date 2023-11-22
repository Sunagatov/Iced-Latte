package com.zufar.icedlatte.user.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class InvalidOldPasswordException extends RuntimeException {

    private final String userName;

    public InvalidOldPasswordException(UUID userId) {
        super(String.format("User with id = %s provided incorrect password.", userId));
        this.userName = null;
    }
}
