package com.zufar.icedlatte.security.jwt.exception;

import org.springframework.security.core.AuthenticationException;

public final class JwtTokenBlacklistedException extends AuthenticationException {

    private final boolean compromiseDetected;

    public JwtTokenBlacklistedException(String message) {
        this(message, false);
    }

    private JwtTokenBlacklistedException(String message, boolean compromiseDetected) {
        super(message);
        this.compromiseDetected = compromiseDetected;
    }

    public static JwtTokenBlacklistedException compromiseDetected(String message) {
        return new JwtTokenBlacklistedException(message, true);
    }

    public boolean isCompromiseDetected() {
        return compromiseDetected;
    }
}
