package com.zufar.icedlatte.security.exception.signin;

import com.zufar.icedlatte.security.exception.AuthException;

public final class InvalidCredentialsException extends AuthException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }

    public InvalidCredentialsException(Throwable cause) {
        super("Invalid credentials", cause);
    }
}
