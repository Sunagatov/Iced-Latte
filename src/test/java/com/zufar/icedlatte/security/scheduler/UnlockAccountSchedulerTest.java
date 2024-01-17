package com.zufar.icedlatte.security.scheduler;

import com.zufar.icedlatte.security.repository.LoginAttemptRepository;
import com.zufar.icedlatte.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnlockAccountScheduler Tests")
class UnlockAccountSchedulerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @InjectMocks
    private UnlockAccountScheduler unlockAccountScheduler;

    @Test
    @DisplayName("Should execute scheduled task to unlock accounts")
    void shouldExecuteScheduledTaskToUnlockAccounts() {
        unlockAccountScheduler.unlockLockoutExpiredAccounts();

        verify(loginAttemptRepository, times(1)).resetLockedAccounts();
        verify(userRepository, times(1)).unlockUsers();
        verifyNoMoreInteractions(loginAttemptRepository, userRepository);
    }
}
