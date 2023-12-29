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

import java.time.LocalDateTime;

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
        LocalDateTime expirationDatetime = LocalDateTime.now().plusMinutes(userAccountLockoutDurationMinutes);
        loginAttemptRepository.setUserLockedStatusAndExpiration(userEmail, expirationDatetime);
        userRepository.setAccountLockedStatus(userEmail, false);
        log.warn("User {} is locked out due to excessive failed login attempts. Lockout duration: {} minutes", userEmail, userAccountLockoutDurationMinutes);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void unlockUserAccount(String userEmail) {
        userRepository.setAccountLockedStatus(userEmail, true);
        log.info("User account with the email = '{}' has been unlocked.", userEmail);
    }
}
