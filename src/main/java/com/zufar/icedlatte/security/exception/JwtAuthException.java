package com.zufar.icedlatte.security.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Sealed base for JWT-related authentication exceptions that integrate with Spring Security.
 * Enables exhaustive pattern matching in switch expressions (Java 25).
 */
public sealed class JwtAuthException extends AuthenticationException
        permits JwtTokenBlacklistedException, JwtTokenHasNoUserEmailException {

    protected JwtAuthException(String message) {
        super(message);
    }

    protected JwtAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
