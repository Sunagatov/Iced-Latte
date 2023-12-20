package com.zufar.icedlatte.email.exception;

public class TokenTimeExpiredException extends RuntimeException {

    public TokenTimeExpiredException() {
        super(String.format("Unfortunately token will be expired"));
    }
}
