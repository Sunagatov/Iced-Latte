package com.zufar.icedlatte.email.exception;

public class TokenTimeExpiredException extends RuntimeException {

    public TokenTimeExpiredException() {
        super("Unfortunately token will be expired");
    }
}
