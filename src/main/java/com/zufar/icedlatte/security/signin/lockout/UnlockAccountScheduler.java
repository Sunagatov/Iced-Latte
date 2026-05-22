package com.zufar.icedlatte.security.signin.lockout;

import com.zufar.icedlatte.common.monitoring.SentryJobMonitor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UnlockAccountScheduler {

    private static final String MONITOR_SLUG = "unlock-account-scheduler";

    private final LoginAttemptService loginAttemptService;
    private final SentryJobMonitor sentryJobMonitor;

    @Value("${unlock-account-scheduler-cron}")
    private String cron;

    @Scheduled(cron = "${unlock-account-scheduler-cron}")
    public void unlockLockoutExpiredAccounts() {
        sentryJobMonitor.run(
                MONITOR_SLUG,
                sentryJobMonitor.cronConfig(cron),
                loginAttemptService::unlockExpiredAccounts
        );
    }
}
