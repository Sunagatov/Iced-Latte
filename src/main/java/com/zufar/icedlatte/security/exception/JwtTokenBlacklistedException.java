package com.zufar.icedlatte.security.exception;

public final class JwtTokenBlacklistedException extends JwtAuthException {

    public JwtTokenBlacklistedException(String message) {
        super(message);
    }
}
