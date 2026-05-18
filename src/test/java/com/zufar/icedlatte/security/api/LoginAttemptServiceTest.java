package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.entity.LoginAttemptEntity;
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
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginAttemptService unit tests")
class LoginAttemptServiceTest {

    @Mock private LoginAttemptRepository loginAttemptRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private LoginAttemptService service;

    private static final String USER_EMAIL = "user@example.com";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxLoginAttempts", MAX_LOGIN_ATTEMPTS);
        ReflectionTestUtils.setField(service, "userAccountLockoutDurationMinutes", LOCKOUT_MINUTES);
    }

    @Nested
    @DisplayName("recordFailure")
    class RecordFailure {

        @Test
        @DisplayName("increments existing login attempt without locking below threshold")
        void incrementsExistingLoginAttemptWithoutLockingBelowThreshold() {
            Instant previousTimestamp = Instant.now().minusSeconds(60);
            LoginAttemptEntity existingAttempt = LoginAttemptEntity.builder()
                    .id(UUID.randomUUID())
                    .userEmail(USER_EMAIL)
                    .attempts(3)
                    .isUserLocked(false)
                    .lastModified(previousTimestamp)
                    .build();
            when(loginAttemptRepository.findByUserEmail(USER_EMAIL)).thenReturn(Optional.of(existingAttempt));
            when(loginAttemptRepository.save(existingAttempt)).thenReturn(existingAttempt);

            service.recordFailure(USER_EMAIL);

            assertThat(existingAttempt.getAttempts()).isEqualTo(4);
            assertThat(existingAttempt.getLastModified()).isAfter(previousTimestamp);
            verify(loginAttemptRepository).findByUserEmail(USER_EMAIL);
            verify(loginAttemptRepository).save(existingAttempt);
            verify(userRepository, never()).setAccountLockedStatus(any(), any(Boolean.class));
            verifyNoMoreInteractions(loginAttemptRepository, userRepository);
        }

        @Test
        @DisplayName("creates a new login attempt record when missing")
        void createsNewLoginAttemptRecordWhenMissing() {
            ArgumentCaptor<LoginAttemptEntity> captor = ArgumentCaptor.forClass(LoginAttemptEntity.class);
            when(loginAttemptRepository.findByUserEmail(USER_EMAIL)).thenReturn(Optional.empty());
            when(loginAttemptRepository.save(any(LoginAttemptEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            service.recordFailure(USER_EMAIL);

            verify(loginAttemptRepository).findByUserEmail(USER_EMAIL);
            verify(loginAttemptRepository).save(captor.capture());
            verify(userRepository, never()).setAccountLockedStatus(any(), any(Boolean.class));
            verifyNoMoreInteractions(loginAttemptRepository, userRepository);

            LoginAttemptEntity saved = captor.getValue();
            assertThat(saved.getUserEmail()).isEqualTo(USER_EMAIL);
            assertThat(saved.getAttempts()).isEqualTo(1);
            assertThat(saved.getIsUserLocked()).isFalse();
            assertThat(saved.getLastModified()).isNotNull();
        }

        @Test
        @DisplayName("locks account when attempts reach threshold")
        void locksAccountWhenAttemptsReachThreshold() {
            LoginAttemptEntity existingAttempt = LoginAttemptEntity.builder()
                    .id(UUID.randomUUID())
                    .userEmail(USER_EMAIL)
                    .attempts(4)
                    .isUserLocked(false)
                    .lastModified(Instant.now().minusSeconds(60))
                    .build();
            ArgumentCaptor<Instant> expirationCaptor = ArgumentCaptor.forClass(Instant.class);
            when(loginAttemptRepository.findByUserEmail(USER_EMAIL)).thenReturn(Optional.of(existingAttempt));
            when(loginAttemptRepository.save(existingAttempt)).thenReturn(existingAttempt);
            when(loginAttemptRepository.setUserLockedStatusAndExpiration(eq(USER_EMAIL), any(Instant.class))).thenReturn(1);
            when(userRepository.setAccountLockedStatus(USER_EMAIL, false)).thenReturn(1);

            Instant before = Instant.now();
            service.recordFailure(USER_EMAIL);
            Instant after = Instant.now();

            verify(loginAttemptRepository).setUserLockedStatusAndExpiration(eq(USER_EMAIL), expirationCaptor.capture());
            verify(userRepository).setAccountLockedStatus(USER_EMAIL, false);
            verifyNoMoreInteractions(loginAttemptRepository, userRepository);

            Instant expiration = expirationCaptor.getValue();
            assertThat(expiration)
                    .isAfterOrEqualTo(before.plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES))
                    .isBeforeOrEqualTo(after.plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES));
        }
    }

    @Nested
    @DisplayName("resetAfterSuccessfulAuthentication")
    class ResetAfterSuccessfulAuthentication {

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
            when(userRepository.setAccountLockedStatus(USER_EMAIL, true)).thenReturn(1);

            service.resetAfterSuccessfulAuthentication(USER_EMAIL);

            assertThat(attempt.getAttempts()).isZero();
            assertThat(attempt.getIsUserLocked()).isFalse();
            assertThat(attempt.getExpirationDatetime()).isNull();
            assertThat(attempt.getLastModified()).isAfter(lastModified);
            verify(loginAttemptRepository).findByUserEmail(USER_EMAIL);
            verify(userRepository).setAccountLockedStatus(USER_EMAIL, true);
            verify(loginAttemptRepository, never()).save(any());
            verifyNoMoreInteractions(loginAttemptRepository, userRepository);
        }

        @Test
        @DisplayName("clears attempts without unlock when already unlocked")
        void clearsAttemptsWithoutUnlockWhenAlreadyUnlocked() {
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

            service.resetAfterSuccessfulAuthentication(USER_EMAIL);

            assertThat(attempt.getAttempts()).isZero();
            assertThat(attempt.getIsUserLocked()).isFalse();
            assertThat(attempt.getExpirationDatetime()).isNull();
            assertThat(attempt.getLastModified()).isAfter(lastModified);
            verify(loginAttemptRepository).findByUserEmail(USER_EMAIL);
            verify(userRepository, never()).setAccountLockedStatus(any(), any(Boolean.class));
            verifyNoMoreInteractions(loginAttemptRepository, userRepository);
        }
    }

    @Test
    @DisplayName("unlockExpiredAccounts resets stale lock records and unlocks users")
    void unlockExpiredAccountsResetsStaleLockRecordsAndUnlocksUsers() {
        when(loginAttemptRepository.resetLockedAccounts()).thenReturn(2);

        service.unlockExpiredAccounts();

        verify(loginAttemptRepository).resetLockedAccounts();
        verify(userRepository).unlockUsers();
        verifyNoMoreInteractions(loginAttemptRepository, userRepository);
    }

    @Test
    @DisplayName("unlockExpiredAccounts propagates database failures for scheduler monitoring")
    void unlockExpiredAccountsPropagatesDatabaseFailures() {
        DataAccessResourceFailureException exception = new DataAccessResourceFailureException("database unavailable");
        when(loginAttemptRepository.resetLockedAccounts()).thenThrow(exception);

        assertThatThrownBy(service::unlockExpiredAccounts).isSameAs(exception);

        verify(loginAttemptRepository).resetLockedAccounts();
        verifyNoMoreInteractions(loginAttemptRepository, userRepository);
    }
}
