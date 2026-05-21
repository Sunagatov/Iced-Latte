package com.zufar.icedlatte.security.exception;

public final class UserRegistrationException extends AuthSecurityException {

    public UserRegistrationException(final String message) {
        super(message);
    }

    public UserRegistrationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
