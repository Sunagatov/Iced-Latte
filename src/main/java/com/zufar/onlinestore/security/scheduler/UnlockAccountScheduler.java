package com.zufar.onlinestore.security.scheduler;

import com.zufar.onlinestore.security.repository.LoginAttemptRepository;
import com.zufar.onlinestore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnlockAccountScheduler {

    private final UserRepository userRepository;
    private final LoginAttemptRepository loginAttemptRepository;

    @Scheduled(cron = "${unlock-account-scheduler-cron}")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void unlockLockoutExpiredAccounts() {
        try {
            log.info("Starting unlockLockoutExpiredAccounts scheduled task.");

            loginAttemptRepository.resetLockedAccounts();
            userRepository.unlockUsers();

            log.info("Finished unlockLockoutExpiredAccounts scheduled task.");
        } catch (Exception exception) {
            log.error("Error during unlockLockoutExpiredAccounts scheduled task.", exception);
        }
    }
}
