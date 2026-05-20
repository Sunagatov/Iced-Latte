package com.zufar.icedlatte.security.exception.jwt;

public final class JwtTokenHasNoUserEmailException extends JwtAuthException {

    public JwtTokenHasNoUserEmailException(String message) {
        super(message);
    }

    public JwtTokenHasNoUserEmailException(String message, Throwable cause) {
        super(message, cause);
    }
}
