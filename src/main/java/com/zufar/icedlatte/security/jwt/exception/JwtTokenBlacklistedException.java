package com.zufar.icedlatte.security.jwt.exception;

import org.springframework.security.core.AuthenticationException;

public final class JwtTokenBlacklistedException extends AuthenticationException {

    public JwtTokenBlacklistedException(String message) {
        super(message);
    }
}
