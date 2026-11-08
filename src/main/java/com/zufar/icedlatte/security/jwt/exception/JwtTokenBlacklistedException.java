package com.zufar.icedlatte.security.jwt.exception;

import org.springframework.security.core.AuthenticationException;

public final class JwtTokenBlacklistedException extends AuthenticationException {

    private final boolean managedSessionRejection;

    public JwtTokenBlacklistedException(String message) {
        this(message, false);
    }

    private JwtTokenBlacklistedException(String message, boolean managedSessionRejection) {
        super(message);
        this.managedSessionRejection = managedSessionRejection;
    }

    public static JwtTokenBlacklistedException managedSessionRejected(String message) {
        return new JwtTokenBlacklistedException(message, true);
    }

    public boolean isManagedSessionRejection() {
        return managedSessionRejection;
    }
}
