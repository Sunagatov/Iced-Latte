package com.zufar.onlinestore.security.signin.attempts;

import com.zufar.onlinestore.security.signin.attempts.entity.LoginAttemptEntity;
import com.zufar.onlinestore.security.signin.attempts.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FailedLoginHandler {

    @Value("${login-attempts.max-attempts}")
    private int maxLoginAttempts;

    @Value("${login-attempts.lockout-duration-minutes}")
    private int lockoutDurationMinutes;

    @Value("${login-attempts.initial-attempts-count}")
    private int initialLoginAttemptsCount;

    private final LoginAttemptRepository loginAttemptRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void handle(final String userEmail) {
        LoginAttemptEntity loginAttempt = loginAttemptRepository.findByUserEmail(userEmail)
                .orElseGet(() -> {
                    LoginAttemptEntity newLoginAttempt = new LoginAttemptEntity();
                    newLoginAttempt.setUserEmail(userEmail);
                    newLoginAttempt.setAttempts(initialLoginAttemptsCount);
                    return newLoginAttempt;
                });

        Integer loginAttemptsCount = loginAttempt.getAttempts();

        loginAttempt.setAttempts(loginAttemptsCount + 1);
        loginAttempt.setLastModified(LocalDateTime.now());

        log.warn("Failed login attempt for user {}, attempts: {}, remaining attempts: {}", userEmail, loginAttemptsCount, maxLoginAttempts - loginAttemptsCount);

        if (loginAttemptsCount >= maxLoginAttempts) {
            loginAttempt.setIsUserLocked(true);
            loginAttempt.setExpirationDatetime(LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
            log.warn("User {} is locked out due to excessive failed login attempts. Lockout duration: {} minutes", userEmail, lockoutDurationMinutes);
        }

        loginAttemptRepository.save(loginAttempt);
    }
}