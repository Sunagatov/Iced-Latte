package com.zufar.onlinestore.security.signin.attempts;

import com.zufar.onlinestore.security.signin.attempts.entity.LoginAttemptEntity;
import com.zufar.onlinestore.security.exception.AccountLockedException;
import com.zufar.onlinestore.security.signin.attempts.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoginAttemptManager {

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;
    private static final int INITIAL_LOGIN_ATTEMPTS_COUNT = 1;

    private final LoginAttemptRepository loginAttemptRepository;

    public void handleFailedLogin(final String userEmail) {
        Optional<LoginAttemptEntity> existingAttempt = loginAttemptRepository.findByUserEmail(userEmail);

        LoginAttemptEntity loginAttempt;
        if (existingAttempt.isEmpty()) {
            loginAttempt = new LoginAttemptEntity();
            loginAttempt.setUserEmail(userEmail);
            loginAttempt.setAttempts(INITIAL_LOGIN_ATTEMPTS_COUNT);
        } else {
            loginAttempt = existingAttempt.get();
            loginAttempt.setAttempts(loginAttempt.getAttempts() + 1);
        }

        loginAttempt.setLastModified(LocalDateTime.now());

        if (loginAttempt.getAttempts() >= MAX_LOGIN_ATTEMPTS) {
            loginAttempt.setIsUserLocked(true);
            loginAttempt.setExpirationDatetime(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
        }

        loginAttemptRepository.save(loginAttempt);
    }

    public void resetFailedLoginAttempts(final String userEmail) {
        loginAttemptRepository.deleteByUserEmail(userEmail);
    }

    public void validateUserLoginLockout(final String userEmail) {
        LocalDateTime lockoutExpiration = loginAttemptRepository.findByUserEmail(userEmail)
                .map(LoginAttemptEntity::getExpirationDatetime)
                .orElse(null);

        // Check if the lockout has expired
        if (lockoutExpiration != null && LocalDateTime.now().isBefore(lockoutExpiration)) {
            throw new AccountLockedException("Your account is locked.");
        }
    }
}
