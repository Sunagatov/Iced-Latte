package com.zufar.icedlatte.user.exception;

public class InvalidOldPasswordException extends RuntimeException {

    public InvalidOldPasswordException() {
        super("Current password is incorrect.");
    }
}
