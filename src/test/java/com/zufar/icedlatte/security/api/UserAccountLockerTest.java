package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.repository.LoginAttemptRepository;
import com.zufar.icedlatte.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAccountLocker unit tests")
class UserAccountLockerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @InjectMocks
    private UserAccountLocker locker;

    private static final String USER_EMAIL = "test@example.com";
    private static final int LOCKOUT_MINUTES = 15;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(locker, "userAccountLockoutDurationMinutes", LOCKOUT_MINUTES);
    }

    @Nested
    @DisplayName("lockUserAccount")
    class LockUserAccount {

        @Test
        @DisplayName("sets lock expiration and marks user as locked")
        void setsLockExpirationAndMarksUserAsLocked() {
            ArgumentCaptor<Instant> expirationCaptor = ArgumentCaptor.forClass(Instant.class);
            when(loginAttemptRepository.setUserLockedStatusAndExpiration(eq(USER_EMAIL), any(Instant.class))).thenReturn(1);
            when(userRepository.setAccountLockedStatus(USER_EMAIL, false)).thenReturn(1);

            Instant before = Instant.now();
            locker.lockUserAccount(USER_EMAIL);
            Instant after = Instant.now();

            verify(loginAttemptRepository).setUserLockedStatusAndExpiration(eq(USER_EMAIL), expirationCaptor.capture());
            verify(userRepository).setAccountLockedStatus(USER_EMAIL, false);
            verifyNoMoreInteractions(loginAttemptRepository, userRepository);

            Instant expiration = expirationCaptor.getValue();
            assertThat(expiration)
                    .isAfterOrEqualTo(before.plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES))
                    .isBeforeOrEqualTo(after.plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES));
        }

        @Test
        @DisplayName("still updates both repositories when rows updated are zero")
        void stillUpdatesBothRepositoriesWhenRowsUpdatedAreZero() {
            when(loginAttemptRepository.setUserLockedStatusAndExpiration(eq(USER_EMAIL), any(Instant.class))).thenReturn(0);
            when(userRepository.setAccountLockedStatus(USER_EMAIL, false)).thenReturn(0);

            locker.lockUserAccount(USER_EMAIL);

            verify(loginAttemptRepository).setUserLockedStatusAndExpiration(eq(USER_EMAIL), any(Instant.class));
            verify(userRepository).setAccountLockedStatus(USER_EMAIL, false);
            verifyNoMoreInteractions(loginAttemptRepository, userRepository);
        }
    }

    @Nested
    @DisplayName("unlockUserAccount")
    class UnlockUserAccount {

        @Test
        @DisplayName("marks user as unlocked")
        void marksUserAsUnlocked() {
            when(userRepository.setAccountLockedStatus(USER_EMAIL, true)).thenReturn(1);

            locker.unlockUserAccount(USER_EMAIL);

            verify(userRepository).setAccountLockedStatus(USER_EMAIL, true);
            verifyNoMoreInteractions(userRepository, loginAttemptRepository);
        }

        @Test
        @DisplayName("still attempts unlock when no rows are updated")
        void stillAttemptsUnlockWhenNoRowsAreUpdated() {
            when(userRepository.setAccountLockedStatus(USER_EMAIL, true)).thenReturn(0);

            locker.unlockUserAccount(USER_EMAIL);

            verify(userRepository).setAccountLockedStatus(USER_EMAIL, true);
            verifyNoMoreInteractions(userRepository, loginAttemptRepository);
        }
    }

}
