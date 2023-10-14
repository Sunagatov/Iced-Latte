package com.zufar.onlinestore.security.signin.attempts;

import com.zufar.onlinestore.security.exception.AccountLockedException;
import com.zufar.onlinestore.security.signin.attempts.entity.LoginAttemptEntity;
import com.zufar.onlinestore.security.signin.attempts.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
            throw new AccountLockedException("Your account is locked.");
        }
    }
}

