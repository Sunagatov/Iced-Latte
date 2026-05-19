package com.zufar.icedlatte.user.exception;

/**
 * Sealed base for all user-related exceptions.
 * Enables exhaustive pattern matching in switch expressions (Java 25).
 */
public abstract sealed class UserException extends RuntimeException
        permits UserNotFoundException, InvalidAvatarFileTypeException {

    protected UserException(String message) {
        super(message);
    }
}
