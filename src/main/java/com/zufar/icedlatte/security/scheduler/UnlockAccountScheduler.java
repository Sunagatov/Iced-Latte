package com.zufar.icedlatte.security.scheduler;

import com.zufar.icedlatte.security.repository.LoginAttemptRepository;
import com.zufar.icedlatte.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
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
            log.debug("Starting unlockLockoutExpiredAccounts scheduled task.");

            int released = loginAttemptRepository.resetLockedAccounts();
            log.debug("Released {} locked login attempt records.", released);
            userRepository.unlockUsers();

            log.debug("Finished unlockLockoutExpiredAccounts scheduled task.");
        } catch (DataAccessException dae) {
            log.error("Database error during unlockLockoutExpiredAccounts scheduled task.", dae);
        } catch (RuntimeException re) {
            log.error("Unexpected error during unlockLockoutExpiredAccounts scheduled task.", re);
        }
    }
}
