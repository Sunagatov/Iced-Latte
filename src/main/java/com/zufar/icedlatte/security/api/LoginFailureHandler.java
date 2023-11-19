package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.entity.LoginAttemptEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginFailureHandler {

    @Value("${login-attempts.max-attempts}")
    private int maxLoginAttempts;

    private final FailedLoginAttemptIncrementor failedLoginAttemptIncrementor;
    private final UserAccountLocker userAccountLocker;

    /**
     * Handles a failed login attempt. If the number of failed attempts surpasses
     * the maximum allowed, the user account is locked.
     *
     * @param userEmail the email of the user for whom the login attempt failed
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void handle(final String userEmail) {
        LoginAttemptEntity loginAttempt = failedLoginAttemptIncrementor.increment(userEmail);

        if (loginAttempt.getAttempts() >= maxLoginAttempts) {
            userAccountLocker.lockUserAccount(userEmail);
        }
        log.warn("Failed login attempt for user {}, attempts: {}, remaining attempts: {}",
                userEmail, loginAttempt.getAttempts(), maxLoginAttempts - loginAttempt.getAttempts());
    }
}

