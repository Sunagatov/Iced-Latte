package com.zufar.onlinestore.security.signin.attempts;

import com.zufar.onlinestore.security.signin.attempts.entity.LoginAttemptEntity;
import com.zufar.onlinestore.security.signin.attempts.exception.LoginAttemptNotFoundException;
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
        LoginAttemptEntity loginAttempt = loginAttemptRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new LoginAttemptNotFoundException(userEmail));

        loginAttempt.setAttempts(initialLoginAttemptsCount);
        loginAttempt.setIsUserLocked(false);
        loginAttempt.setExpirationDatetime(null);
        loginAttempt.setLastModified(LocalDateTime.now());

        loginAttemptRepository.save(loginAttempt);
    }
}
