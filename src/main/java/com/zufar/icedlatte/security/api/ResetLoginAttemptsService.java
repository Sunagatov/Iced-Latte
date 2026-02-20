package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResetLoginAttemptsService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final UserAccountLocker userAccountLocker;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void reset(final String userEmail) {
        loginAttemptRepository.findByUserEmail(userEmail)
                .ifPresent(existingLoginAttempt -> {
                    userAccountLocker.unlockUserAccount(userEmail);
                    existingLoginAttempt.setAttempts(0);
                    existingLoginAttempt.setIsUserLocked(false);
                    existingLoginAttempt.setExpirationDatetime(null);
                    existingLoginAttempt.setLastModified(java.time.LocalDateTime.now());
                    loginAttemptRepository.save(existingLoginAttempt);
                    log.info("Login attempts reset for user {}.", userEmail);
                });
    }
}
