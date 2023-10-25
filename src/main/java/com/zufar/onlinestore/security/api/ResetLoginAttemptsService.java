package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.security.entity.LoginAttemptEntity;
import com.zufar.onlinestore.security.repository.LoginAttemptRepository;
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
        userAccountLocker.unlockUserAccount(userEmail);

        loginAttemptRepository.findByUserEmail(userEmail)
                .ifPresent(loginAttempt -> {
                    LoginAttemptEntity newLoginAttempt = LoginAttemptFactory.createInitialFailedLoggedAttemptEntity(userEmail);
                    newLoginAttempt.setId(loginAttempt.getId());

                    loginAttemptRepository.save(newLoginAttempt);
                    log.info("Login attempts reset for user {}.", userEmail);
                });
    }
}
