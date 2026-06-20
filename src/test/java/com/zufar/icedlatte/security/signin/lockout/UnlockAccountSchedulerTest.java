package com.zufar.icedlatte.security.signin.lockout;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.zufar.icedlatte.common.monitoring.SentryJobMonitor;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnlockAccountScheduler Tests")
class UnlockAccountSchedulerTest {

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private SentryJobMonitor sentryJobMonitor;

    @Test
    @DisplayName("Should execute scheduled task to unlock accounts")
    void shouldExecuteScheduledTaskToUnlockAccounts() {
        doAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(2)).run();
                    return null;
                })
                .when(sentryJobMonitor)
                .run(eq("unlock-account-scheduler"), any(), any(Runnable.class));
        UnlockAccountScheduler unlockAccountScheduler =
                new UnlockAccountScheduler(loginAttemptService, sentryJobMonitor);
        ReflectionTestUtils.setField(unlockAccountScheduler, "cron", "0 0/5 * * * *");

        unlockAccountScheduler.unlockLockoutExpiredAccounts();

        verify(loginAttemptService).unlockExpiredAccounts();
        verify(sentryJobMonitor).run(eq("unlock-account-scheduler"), any(), any(Runnable.class));
    }
}
