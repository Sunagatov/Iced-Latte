package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.entity.LoginAttemptEntity;
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
public class LoginAttemptService {

    @Value("${login-attempts.max-attempts}")
    private int maxLoginAttempts;

    @Value("${login-attempts.lockout-duration-minutes}")
    private int userAccountLockoutDurationMinutes;

    private final LoginAttemptRepository loginAttemptRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void recordFailure(String userEmail) {
        LoginAttemptEntity loginAttempt = loginAttemptRepository.findByUserEmail(userEmail)
                .orElseGet(() -> {
                    log.debug("auth.login_attempts.new_record");
                    return LoginAttemptEntity.builder()
                            .userEmail(userEmail)
                            .attempts(0)
                            .isUserLocked(false)
                            .lastModified(Instant.now())
                            .build();
                });

        loginAttempt.setAttempts(loginAttempt.getAttempts() + 1);
        loginAttempt.setLastModified(Instant.now());
        loginAttemptRepository.save(loginAttempt);

        int remaining = Math.max(0, maxLoginAttempts - loginAttempt.getAttempts());
        if (loginAttempt.getAttempts() >= maxLoginAttempts) {
            lockUserAccount(userEmail);
        }
        log.debug("login.failed: attempts={}, remaining={}", loginAttempt.getAttempts(), remaining);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void resetAfterSuccessfulAuthentication(String userEmail) {
        loginAttemptRepository.findByUserEmail(userEmail)
                .ifPresent(existingLoginAttempt -> {
                    if (Boolean.TRUE.equals(existingLoginAttempt.getIsUserLocked())) {
                        unlockUserAccount(userEmail);
                    }
                    existingLoginAttempt.setAttempts(0);
                    existingLoginAttempt.setIsUserLocked(false);
                    existingLoginAttempt.setExpirationDatetime(null);
                    existingLoginAttempt.setLastModified(Instant.now());
                    log.debug("auth.login_attempts.reset");
                });
    }

    public void unlockExpiredAccounts() {
        log.debug("scheduler.unlock.start");

        int released = loginAttemptRepository.resetLockedAccounts();
        log.debug("scheduler.unlock.released: count={}", released);
        userRepository.unlockUsers();

        log.debug("scheduler.unlock.finish");
    }

    private void lockUserAccount(String userEmail) {
        Instant expirationDatetime = Instant.now().plus(userAccountLockoutDurationMinutes, ChronoUnit.MINUTES);
        int attemptRows = loginAttemptRepository.setUserLockedStatusAndExpiration(userEmail, expirationDatetime);
        int userRows = userRepository.setAccountLockedStatus(userEmail, false);
        if (attemptRows == 0 || userRows == 0) {
            log.error("auth.account.lock_failed: loginAttemptRows={}, userRows={}, message=no rows updated",
                    attemptRows, userRows);
        } else {
            log.warn("auth.account.locked: reasonCode=MAX_LOGIN_ATTEMPTS, durationMinutes={}",
                    userAccountLockoutDurationMinutes);
        }
    }

    private void unlockUserAccount(String userEmail) {
        int userRows = userRepository.setAccountLockedStatus(userEmail, true);
        if (userRows == 0) {
            log.error("auth.account.unlock_failed: userRows={}, message=no rows updated", userRows);
        } else {
            log.info("auth.account.unlocked");
        }
    }
}
