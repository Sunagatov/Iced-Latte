package com.zufar.icedlatte.security.scheduler;

import com.zufar.icedlatte.security.api.LoginAttemptService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnlockAccountScheduler Tests")
class UnlockAccountSchedulerTest {

    @Mock
    private LoginAttemptService loginAttemptService;

    @Test
    @DisplayName("Should execute scheduled task to unlock accounts")
    void shouldExecuteScheduledTaskToUnlockAccounts() {
        UnlockAccountScheduler unlockAccountScheduler = new UnlockAccountScheduler(loginAttemptService,
                new com.zufar.icedlatte.common.monitoring.SentryJobMonitor());
        ReflectionTestUtils.setField(unlockAccountScheduler, "cron", "0 0/5 * * * *");

        unlockAccountScheduler.unlockLockoutExpiredAccounts();

        verify(loginAttemptService).unlockExpiredAccounts();
    }
}
