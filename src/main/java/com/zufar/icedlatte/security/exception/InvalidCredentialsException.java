package com.zufar.icedlatte.security.exception;

public final class InvalidCredentialsException extends AuthSecurityException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }

    public InvalidCredentialsException(Throwable cause) {
        super("Invalid credentials", cause);
    }
}
