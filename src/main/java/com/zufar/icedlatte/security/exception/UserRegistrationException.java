package com.zufar.icedlatte.security.exception;

import lombok.Getter;

@Getter
public class UserRegistrationException extends RuntimeException {

    private final String email;

    public UserRegistrationException(final String email, final String message) {
        super(message);
        this.email = email;
    }

    public UserRegistrationException(final String email, final String message, final Throwable cause) {
        super(message, cause);
        this.email = email;
    }
}