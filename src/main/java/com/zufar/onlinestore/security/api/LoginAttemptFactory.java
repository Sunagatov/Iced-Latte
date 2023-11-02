package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.security.entity.LoginAttemptEntity;

import java.time.LocalDateTime;

public class LoginAttemptFactory {

    public static LoginAttemptEntity createInitialFailedLoggedAttemptEntity(String userEmail) {
        int attempts = 0;
        LocalDateTime lastModified = LocalDateTime.now();
        boolean isUserLocked = false;
        return new LoginAttemptEntity(null, userEmail, attempts, null, isUserLocked, lastModified);
    }
}
