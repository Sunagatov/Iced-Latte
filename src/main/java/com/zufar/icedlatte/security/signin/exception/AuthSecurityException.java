package com.zufar.icedlatte.security.signin.exception;

/**
 * Sealed base for security exceptions owned by this module. Enables exhaustive pattern matching in the exception
 * handler.
 */
public abstract sealed class AuthSecurityException extends RuntimeException
        permits AbsentBearerHeaderException,
                UserRegistrationException,
                InvalidCredentialsException,
                UserAccountLockedException,
                SessionNotFoundException,
                SessionOwnershipException {

    protected AuthSecurityException(String message) {
        super(message);
    }

    protected AuthSecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
