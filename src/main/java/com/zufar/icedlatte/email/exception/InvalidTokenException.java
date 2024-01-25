package com.zufar.icedlatte.email.exception;

import lombok.Getter;

@Getter
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String email) {
        super(String.format("Invalid token for email = %s", email));
    }
}
