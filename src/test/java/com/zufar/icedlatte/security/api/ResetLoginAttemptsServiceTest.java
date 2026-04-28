package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.entity.LoginAttemptEntity;
import com.zufar.icedlatte.security.repository.LoginAttemptRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResetLoginAttemptsService unit tests")
class ResetLoginAttemptsServiceTest {

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private UserAccountLocker userAccountLocker;

    @InjectMocks
    private ResetLoginAttemptsService service;

    private static final String USER_EMAIL = "user@example.com";

    @Nested
    @DisplayName("reset")
    class Reset {

        @Test
        @DisplayName("clears attempts and unlocks locked user")
        void clearsAttemptsAndUnlocksLockedUser() {
            Instant expiration = Instant.now().plusSeconds(600);
            Instant lastModified = Instant.now().minusSeconds(120);
            LoginAttemptEntity attempt = LoginAttemptEntity.builder()
                    .id(UUID.randomUUID())
                    .userEmail(USER_EMAIL)
                    .attempts(3)
                    .isUserLocked(true)
                    .expirationDatetime(expiration)
                    .lastModified(lastModified)
                    .build();
            when(loginAttemptRepository.findByUserEmail(USER_EMAIL)).thenReturn(Optional.of(attempt));

            service.reset(USER_EMAIL);

            assertThat(attempt.getAttempts()).isZero();
            assertThat(attempt.getIsUserLocked()).isFalse();
            assertThat(attempt.getExpirationDatetime()).isNull();
            assertThat(attempt.getLastModified()).isAfter(lastModified);
            verify(loginAttemptRepository).findByUserEmail(USER_EMAIL);
            verify(userAccountLocker).unlockUserAccount(USER_EMAIL);
            verify(loginAttemptRepository, never()).save(any());
            verifyNoMoreInteractions(loginAttemptRepository, userAccountLocker);
        }

        @Test
        @DisplayName("clears attempts without calling unlock for already unlocked user")
        void clearsAttemptsWithoutCallingUnlockForUnlockedUser() {
            Instant lastModified = Instant.now().minusSeconds(120);
            LoginAttemptEntity attempt = LoginAttemptEntity.builder()
                    .id(UUID.randomUUID())
                    .userEmail(USER_EMAIL)
                    .attempts(2)
                    .isUserLocked(false)
                    .expirationDatetime(Instant.now().plusSeconds(600))
                    .lastModified(lastModified)
                    .build();
            when(loginAttemptRepository.findByUserEmail(USER_EMAIL)).thenReturn(Optional.of(attempt));

            service.reset(USER_EMAIL);

            assertThat(attempt.getAttempts()).isZero();
            assertThat(attempt.getIsUserLocked()).isFalse();
            assertThat(attempt.getExpirationDatetime()).isNull();
            assertThat(attempt.getLastModified()).isAfter(lastModified);
            verify(loginAttemptRepository).findByUserEmail(USER_EMAIL);
            verify(userAccountLocker, never()).unlockUserAccount(USER_EMAIL);
            verify(loginAttemptRepository, never()).save(any());
            verifyNoMoreInteractions(loginAttemptRepository, userAccountLocker);
        }

        @Test
        @DisplayName("does nothing when login attempt record is missing")
        void doesNothingWhenLoginAttemptRecordIsMissing() {
            when(loginAttemptRepository.findByUserEmail(USER_EMAIL)).thenReturn(Optional.empty());

            service.reset(USER_EMAIL);

            verify(loginAttemptRepository).findByUserEmail(USER_EMAIL);
            verify(userAccountLocker, never()).unlockUserAccount(USER_EMAIL);
            verifyNoMoreInteractions(loginAttemptRepository, userAccountLocker);
        }
    }
}
