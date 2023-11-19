package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.entity.LoginAttemptEntity;
import com.zufar.icedlatte.security.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FailedLoginAttemptIncrementor {

    private final LoginAttemptRepository loginAttemptRepository;
    private final LoginAttemptFactory loginAttemptFactory;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public LoginAttemptEntity increment(String userEmail) {
        LoginAttemptEntity loginAttempt = loginAttemptRepository.findByUserEmail(userEmail)
                .orElseGet(() -> {
                    log.info("No login attempt record found for email: {}. Creating a new record.", userEmail);
                    return loginAttemptFactory.createInitialFailedLoggedAttemptEntity(userEmail);
                });
        loginAttempt.setAttempts(loginAttempt.getAttempts() + 1);
        loginAttempt.setLastModified(LocalDateTime.now());
        return loginAttemptRepository.save(loginAttempt);
    }

}
