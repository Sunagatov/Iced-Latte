package com.zufar.icedlatte.security.exception.jwt;

import com.zufar.icedlatte.security.exception.AuthException;

public final class JwtTokenException extends AuthException {

    public JwtTokenException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

    public JwtTokenException(String message) {
        super(message);
    }

    public JwtTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
