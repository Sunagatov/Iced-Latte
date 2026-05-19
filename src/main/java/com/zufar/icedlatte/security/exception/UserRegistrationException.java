package com.zufar.icedlatte.security.exception;

public final class UserRegistrationException extends AuthException {

    public UserRegistrationException(final String message) {
        super(message);
    }

    public UserRegistrationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
