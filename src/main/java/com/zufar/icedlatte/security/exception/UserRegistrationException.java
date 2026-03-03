package com.zufar.icedlatte.security.exception;

public class UserRegistrationException extends RuntimeException {

    public UserRegistrationException(final String message) {
        super(message);
    }

    public UserRegistrationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
