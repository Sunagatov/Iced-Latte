package com.zufar.icedlatte.security.signin.lockout;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.security.signin.repository.LoginAttemptRepository;
import com.zufar.icedlatte.user.api.UserAccessControlApi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final UserAccessControlApi userAccessControlApi;
    private final LoginAttemptProperties properties;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void recordFailure(String userEmail) {
        List<Integer> recordedAttempts = loginAttemptRepository.recordFailedAttempt(userEmail, Instant.now());
        if (recordedAttempts.isEmpty()) {
            log.debug("login.failed_attempt.not_recorded: reason=user_not_found");
            return;
        }

        int attempts = recordedAttempts.getFirst();
        int remaining = Math.max(0, properties.maxAttempts() - attempts);
        if (attempts >= properties.maxAttempts()) {
            lockUserAccount(userEmail);
        }
        log.debug("login.failed: attempts={}, remaining={}", attempts, remaining);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void resetAfterSuccessfulAuthentication(String userEmail) {
        loginAttemptRepository.findByUserEmail(userEmail).ifPresent(existingLoginAttempt -> {
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

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void unlockExpiredAccounts() {
        log.debug("scheduler.unlock.start");

        loginAttemptRepository.findExpiredLockedUserEmails().forEach(this::unlockUserAccount);
        int released = loginAttemptRepository.resetLockedAccounts();
        log.debug("scheduler.unlock.released: count={}", released);

        log.debug("scheduler.unlock.finish");
    }

    private void lockUserAccount(String userEmail) {
        Instant expirationDatetime = Instant.now().plus(properties.lockoutDurationMinutes(), ChronoUnit.MINUTES);
        int attemptRows = loginAttemptRepository.setUserLockedStatusAndExpiration(userEmail, expirationDatetime);
        int userRows = userAccessControlApi.lockAccount(userEmail);
        if (attemptRows == 0 || userRows == 0) {
            log.error(
                    "auth.account.lock_failed: loginAttemptRows={}, userRows={}, message=no rows updated",
                    attemptRows,
                    userRows);
        } else {
            log.warn(
                    "auth.account.locked: reasonCode=MAX_LOGIN_ATTEMPTS, durationMinutes={}",
                    properties.lockoutDurationMinutes());
        }
    }

    private void unlockUserAccount(String userEmail) {
        int userRows = userAccessControlApi.unlockAccount(userEmail);
        if (userRows == 0) {
            log.error("auth.account.unlock_failed: userRows={}, message=no rows updated", userRows);
        } else {
            log.info("auth.account.unlocked");
        }
    }
}
