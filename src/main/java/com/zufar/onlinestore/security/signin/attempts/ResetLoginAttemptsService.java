package com.zufar.onlinestore.security.signin.attempts;

import com.zufar.onlinestore.security.signin.attempts.entity.LoginAttemptEntity;
import com.zufar.onlinestore.security.signin.attempts.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ResetLoginAttemptsService {

    @Value("${login-attempts.initial-attempts-count}")
    private int initialLoginAttemptsCount;

    private final LoginAttemptRepository loginAttemptRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void reset(final String userEmail) {
        LoginAttemptEntity newLoginAttempt = new LoginAttemptEntity();
        newLoginAttempt.setUserEmail(userEmail);
        newLoginAttempt.setAttempts(initialLoginAttemptsCount);
        newLoginAttempt.setIsUserLocked(false);
        newLoginAttempt.setExpirationDatetime(null);
        newLoginAttempt.setLastModified(LocalDateTime.now());

        loginAttemptRepository.save(newLoginAttempt);
    }
}
