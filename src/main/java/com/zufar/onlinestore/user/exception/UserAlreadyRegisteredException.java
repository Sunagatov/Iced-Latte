package com.zufar.onlinestore.user.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class UserAlreadyRegisteredException extends RuntimeException {

    private final List<String> errors;

    public UserAlreadyRegisteredException(List<String> errors) {
        this.errors = errors;
    }
}
