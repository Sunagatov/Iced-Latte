package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.entity.LoginAttemptEntity;
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
    private final LoginAttemptFactory loginAttemptFactory;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void reset(final String userEmail) {
        userAccountLocker.unlockUserAccount(userEmail);

        loginAttemptRepository.findByUserEmail(userEmail)
                .ifPresent(loginAttempt -> {
                    LoginAttemptEntity newLoginAttempt = loginAttemptFactory.createInitialFailedLoggedAttemptEntity(userEmail);
                    newLoginAttempt.setId(loginAttempt.getId());

                    loginAttemptRepository.save(newLoginAttempt);
                    log.info("Login attempts reset for user {}.", userEmail);
                });
    }
}
