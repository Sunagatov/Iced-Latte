package com.zufar.icedlatte.security.signup.exception;

import com.zufar.icedlatte.common.exception.BadRequestException;

public class InvalidEmailTokenException extends BadRequestException {

    public InvalidEmailTokenException() {
        super("This email code is invalid, expired or already used. Request a new email.");
    }
}
