package com.zufar.onlinestore.security.signin.attempts;

import com.zufar.onlinestore.security.signin.attempts.repository.LoginAttemptRepository;
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
public class ResetLoginAttemptsService {

    @Value("${login-attempts.initial-attempts-count}")
    private int initialLoginAttemptsCount;

    private final LoginAttemptRepository loginAttemptRepository;
    private final UserAccountLocker userAccountLocker;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void reset(final String userEmail) {
        userAccountLocker.unlockUserAccount(userEmail);

        loginAttemptRepository.findByUserEmail(userEmail)
                .ifPresent(loginAttempt -> {
                    loginAttempt.setAttempts(initialLoginAttemptsCount);
                    loginAttempt.setIsUserLocked(false);
                    loginAttempt.setExpirationDatetime(null);
                    loginAttempt.setLastModified(LocalDateTime.now());

                    loginAttemptRepository.save(loginAttempt);
                    log.info("Login attempts reset for user {}.", userEmail);
                });
    }
}
