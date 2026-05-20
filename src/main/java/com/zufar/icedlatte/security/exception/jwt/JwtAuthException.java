package com.zufar.icedlatte.security.exception.jwt;

import org.springframework.security.core.AuthenticationException;

/**
 * Sealed base for JWT-related authentication exceptions that integrate with Spring Security.
 * Enables exhaustive pattern matching in switch expressions (Java 25).
 */
public abstract class JwtAuthException extends AuthenticationException {

    protected JwtAuthException(String message) {
        super(message);
    }

    protected JwtAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
