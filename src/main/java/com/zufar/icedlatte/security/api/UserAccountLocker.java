package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.repository.LoginAttemptRepository;
import com.zufar.icedlatte.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccountLocker {

    @Value("${login-attempts.lockout-duration-minutes}")
    private int userAccountLockoutDurationMinutes;

    private final UserRepository userRepository;
    private final LoginAttemptRepository loginAttemptRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void lockUserAccount(String userEmail) {
        Instant expirationDatetime = Instant.now().plus(userAccountLockoutDurationMinutes, ChronoUnit.MINUTES);
        int attemptRows = loginAttemptRepository.setUserLockedStatusAndExpiration(userEmail, expirationDatetime);
        int userRows = userRepository.setAccountLockedStatus(userEmail, false);
        if (attemptRows == 0 || userRows == 0) {
            log.error("Failed to lock account: loginAttemptRows={}, userRows={}", attemptRows, userRows);
        } else {
            log.warn("Account locked due to excessive failed login attempts. Lockout duration: {} minutes", userAccountLockoutDurationMinutes);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void unlockUserAccount(String userEmail) {
        int userRows = userRepository.setAccountLockedStatus(userEmail, true);
        if (userRows == 0) {
            log.error("Failed to unlock account: no rows updated");
        } else {
            log.info("User account has been unlocked.");
        }
    }
}
