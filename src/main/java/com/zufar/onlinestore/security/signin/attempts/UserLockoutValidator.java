package com.zufar.onlinestore.security.signin.attempts;

import com.zufar.onlinestore.security.exception.AccountLockedException;
import com.zufar.onlinestore.security.signin.attempts.entity.LoginAttemptEntity;
import com.zufar.onlinestore.security.signin.attempts.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLockoutValidator {

    private final LoginAttemptRepository loginAttemptRepository;

    @Transactional(readOnly = true)
    public void validate(final String userEmail) {
        LocalDateTime lockoutExpiration = loginAttemptRepository.findByUserEmail(userEmail)
                .map(LoginAttemptEntity::getExpirationDatetime)
                .orElse(null);

        if (lockoutExpiration != null && LocalDateTime.now().isBefore(lockoutExpiration)) {
            log.info("User with email = '{}' is locked. Login is not allowed until '{}'", userEmail, lockoutExpiration);
            throw new AccountLockedException();
        }
    }
}

