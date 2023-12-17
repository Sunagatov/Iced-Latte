package com.zufar.icedlatte.user.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class UserAvatarNotFoundException extends RuntimeException {

    private final UUID userId;

    public UserAvatarNotFoundException(UUID userId) {
        super(String.format("User with userId = '%s' does not have avatar", userId));
        this.userId = userId;
    }
}
