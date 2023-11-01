package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.security.repository.LoginAttemptRepository;
import com.zufar.onlinestore.user.repository.UserRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAccountLockerTest {
    @InjectMocks
    private UserAccountLocker userAccountLocker;
    @Mock
    private UserRepository userRepository;
    @Mock
    private LoginAttemptRepository loginAttemptRepository;
    private String userEmail = Instancio.of(String.class)
            .create();

    @Test
    void testLockUserAccount() {
        userAccountLocker.lockUserAccount(userEmail);
        verify(loginAttemptRepository, times(1))
                .setUserLockedStatusAndExpiration(eq(userEmail), any());
        verify(userRepository, times(1))
                .setAccountLockedStatus(userEmail, false);
    }

    @Test
    void testUnlockUserAccount() {
        userAccountLocker.unlockUserAccount(userEmail);
        verify(userRepository, times(1))
                .setAccountLockedStatus(userEmail, true);
    }

}