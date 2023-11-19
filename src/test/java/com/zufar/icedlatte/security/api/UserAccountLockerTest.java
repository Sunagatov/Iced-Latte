package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.repository.LoginAttemptRepository;
import com.zufar.icedlatte.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    private final String userEmail = "TestEmail";
    private final LocalDateTime timeBeforeRunningMethod = LocalDateTime.now().minusSeconds(1);

    @Test
    @DisplayName("Should Lock User Account When Account Is Not Already Locked")
    void shouldLockUserAccountWhenAccountIsNotLocked() {
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> expirationCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        assertDoesNotThrow(() -> userAccountLocker.lockUserAccount(userEmail));
        verify(loginAttemptRepository, times(1)).setUserLockedStatusAndExpiration(emailCaptor.capture(), expirationCaptor.capture());
        assertEquals(userEmail, emailCaptor.getValue());
        assertTrue(expirationCaptor.getValue().isAfter(timeBeforeRunningMethod));
        verify(userRepository, times(1)).setAccountLockedStatus(userEmail, false);
    }

    @Test
    @DisplayName("Should Unlock User Account When Account Is Locked")
    void shouldUnlockUserAccountWhenAccountIsLocked() {
        assertDoesNotThrow(() -> userAccountLocker.unlockUserAccount(userEmail));
        verify(userRepository, times(1)).setAccountLockedStatus(userEmail, true);
    }
}
