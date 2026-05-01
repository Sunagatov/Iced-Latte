package com.zufar.icedlatte.security.scheduler;

import com.zufar.icedlatte.security.api.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UnlockAccountScheduler {

    private final LoginAttemptService loginAttemptService;

    @Scheduled(cron = "${unlock-account-scheduler-cron}")
    public void unlockLockoutExpiredAccounts() {
        loginAttemptService.unlockExpiredAccounts();
    }
}
