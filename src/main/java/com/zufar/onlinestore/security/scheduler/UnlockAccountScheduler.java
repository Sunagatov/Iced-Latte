package com.zufar.onlinestore.security.scheduler;

import com.zufar.onlinestore.user.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnlockAccountScheduler {

    private final UserRepository userRepository;

    @Scheduled(cron = "${unlock-account-scheduler-cron}")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void unlockLockoutExpiredAccounts() {
        LocalDateTime now = LocalDateTime.now();
        userRepository.unlockLockoutExpiredAccounts(now, now);
    }
}
