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
        Instant expirationDatetime = Instant.now().plus(userAccountLockoutDurationMinutes, java.time.temporal.ChronoUnit.MINUTES);
        int attemptRows = loginAttemptRepository.setUserLockedStatusAndExpiration(userEmail, expirationDatetime);
        int userRows = userRepository.setAccountLockedStatus(userEmail, false);
        if (attemptRows == 0 || userRows == 0) {
            log.error("Failed to lock account for email='{}': loginAttemptRows={}, userRows={}", userEmail, attemptRows, userRows);
        } else {
            log.warn("User {} is locked out due to excessive failed login attempts. Lockout duration: {} minutes", userEmail, userAccountLockoutDurationMinutes);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void unlockUserAccount(String userEmail) {
        int userRows = userRepository.setAccountLockedStatus(userEmail, true);
        if (userRows == 0) {
            log.error("Failed to unlock account for email='{}': no rows updated", userEmail);
        } else {
            log.info("User account with the email = '{}' has been unlocked.", userEmail);
        }
    }
}
