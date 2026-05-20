package com.zufar.icedlatte.security.exception;

/**
 * Sealed base for all authentication/authorization exceptions owned by this project.
 * Enables exhaustive pattern matching in switch expressions (Java 25).
 */
public abstract class AuthException extends RuntimeException {

    protected AuthException(String message) {
        super(message);
    }

    protected AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
