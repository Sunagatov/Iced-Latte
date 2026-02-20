package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.entity.LoginAttemptEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LoginAttemptFactory {

    public LoginAttemptEntity createInitialFailedLoggedAttemptEntity(String userEmail) {
        int attempts = 0;
        LocalDateTime lastModified = LocalDateTime.now();
        boolean isUserLocked = false;
        return new LoginAttemptEntity(null, userEmail, attempts, null, isUserLocked, lastModified);
    }
}
