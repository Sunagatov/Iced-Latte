package com.zufar.icedlatte.user.exception;

public class InvalidOldPasswordException extends RuntimeException {

    public InvalidOldPasswordException(String userEmail) {
        super(String.format("User with userEmail = '%s' provided incorrect password.", userEmail));
    }
}
