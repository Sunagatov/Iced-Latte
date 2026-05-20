package com.zufar.icedlatte.security.exception.signup;

import com.zufar.icedlatte.security.exception.AuthException;

public final class UserRegistrationException extends AuthException {

    public UserRegistrationException(final String message) {
        super(message);
    }

    public UserRegistrationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
