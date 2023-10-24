package com.zufar.onlinestore.security.signin.attempts;

import com.zufar.onlinestore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccountLocker {

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void lockUserAccount(String userEmail) {
        userRepository.setAccountLockedStatus(userEmail, false);
        log.info("User account with the email = '{}' has been locked.", userEmail);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void unlockUserAccount(String userEmail) {
        userRepository.setAccountLockedStatus(userEmail, true);
        log.info("User account with the email = '{}' has been unlocked.", userEmail);
    }
}
