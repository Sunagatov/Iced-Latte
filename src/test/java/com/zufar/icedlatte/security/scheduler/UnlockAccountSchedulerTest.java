package com.zufar.icedlatte.security.scheduler;

import com.zufar.icedlatte.security.api.LoginAttemptService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnlockAccountScheduler Tests")
class UnlockAccountSchedulerTest {

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private UnlockAccountScheduler unlockAccountScheduler;

    @Test
    @DisplayName("Should execute scheduled task to unlock accounts")
    void shouldExecuteScheduledTaskToUnlockAccounts() {
        unlockAccountScheduler.unlockLockoutExpiredAccounts();

        verify(loginAttemptService).unlockExpiredAccounts();
    }
}
