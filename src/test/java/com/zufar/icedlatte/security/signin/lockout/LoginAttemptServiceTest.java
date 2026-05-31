package com.zufar.icedlatte.security.signin.lockout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import com.zufar.icedlatte.security.signin.entity.LoginAttemptEntity;
import com.zufar.icedlatte.security.signin.repository.LoginAttemptRepository;
import com.zufar.icedlatte.user.api.UserAccessControlApi;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginAttemptService unit tests")
class LoginAttemptServiceTest {

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private UserAccessControlApi userAccessControlApi;

    @Mock
    private LoginAttemptProperties properties;

    @InjectMocks
    private LoginAttemptService service;

    private static final String USER_EMAIL = "user@example.com";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    @BeforeEach
    void setUp() {
        lenient().when(properties.maxAttempts()).thenReturn(MAX_LOGIN_ATTEMPTS);
        lenient().when(properties.lockoutDurationMinutes()).thenReturn(LOCKOUT_MINUTES);
    }

    @Nested
    @DisplayName("recordFailure")
    class RecordFailure {

        @Test
        @DisplayName("records failed attempt atomically without locking below threshold")
        void recordsFailedAttemptAtomicallyWithoutLockingBelowThreshold() {
            when(loginAttemptRepository.recordFailedAttempt(eq(USER_EMAIL), any(Instant.class)))
                    .thenReturn(List.of(4));

            service.recordFailure(USER_EMAIL);

            verify(loginAttemptRepository).recordFailedAttempt(eq(USER_EMAIL), any(Instant.class));
            verify(userAccessControlApi, never()).lockAccount(any());
            verifyNoMoreInteractions(loginAttemptRepository, userAccessControlApi);
        }

        @Test
        @DisplayName("records first failed attempt with the same atomic operation")
        void recordsFirstFailedAttemptWithSameAtomicOperation() {
            when(loginAttemptRepository.recordFailedAttempt(eq(USER_EMAIL), any(Instant.class)))
                    .thenReturn(List.of(1));

            service.recordFailure(USER_EMAIL);

            verify(loginAttemptRepository).recordFailedAttempt(eq(USER_EMAIL), any(Instant.class));
            verify(userAccessControlApi, never()).lockAccount(any());
            verifyNoMoreInteractions(loginAttemptRepository, userAccessControlApi);
        }

        @Test
        @DisplayName("locks account when attempts reach threshold")
        void locksAccountWhenAttemptsReachThreshold() {
            ArgumentCaptor<Instant> expirationCaptor = ArgumentCaptor.forClass(Instant.class);
            when(loginAttemptRepository.recordFailedAttempt(eq(USER_EMAIL), any(Instant.class)))
                    .thenReturn(List.of(MAX_LOGIN_ATTEMPTS));
            when(loginAttemptRepository.setUserLockedStatusAndExpiration(eq(USER_EMAIL), any(Instant.class)))
                    .thenReturn(1);
            when(userAccessControlApi.lockAccount(USER_EMAIL)).thenReturn(1);

            Instant before = Instant.now();
            service.recordFailure(USER_EMAIL);
            Instant after = Instant.now();

            verify(loginAttemptRepository).setUserLockedStatusAndExpiration(eq(USER_EMAIL), expirationCaptor.capture());
            verify(userAccessControlApi).lockAccount(USER_EMAIL);
            verifyNoMoreInteractions(loginAttemptRepository, userAccessControlApi);

            Instant expiration = expirationCaptor.getValue();
            assertThat(expiration)
                    .isAfterOrEqualTo(before.plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES))
                    .isBeforeOrEqualTo(after.plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES));
        }

        @Test
        @DisplayName("does not lock or fail when user email is absent")
        void doesNotLockOrFailWhenUserEmailIsAbsent() {
            when(loginAttemptRepository.recordFailedAttempt(eq(USER_EMAIL), any(Instant.class)))
                    .thenReturn(List.of());

            service.recordFailure(USER_EMAIL);

            verify(loginAttemptRepository).recordFailedAttempt(eq(USER_EMAIL), any(Instant.class));
            verify(userAccessControlApi, never()).lockAccount(any());
            verifyNoMoreInteractions(loginAttemptRepository, userAccessControlApi);
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
            when(userAccessControlApi.unlockAccount(USER_EMAIL)).thenReturn(1);

            service.resetAfterSuccessfulAuthentication(USER_EMAIL);

            assertThat(attempt.getAttempts()).isZero();
            assertThat(attempt.getIsUserLocked()).isFalse();
            assertThat(attempt.getExpirationDatetime()).isNull();
            assertThat(attempt.getLastModified()).isAfter(lastModified);
            verify(loginAttemptRepository).findByUserEmail(USER_EMAIL);
            verify(userAccessControlApi).unlockAccount(USER_EMAIL);
            verify(loginAttemptRepository, never()).save(any());
            verifyNoMoreInteractions(loginAttemptRepository, userAccessControlApi);
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
            verify(userAccessControlApi, never()).lockAccount(any());
            verifyNoMoreInteractions(loginAttemptRepository, userAccessControlApi);
        }
    }

    @Test
    @DisplayName("unlockExpiredAccounts resets stale lock records and unlocks users")
    void unlockExpiredAccountsResetsStaleLockRecordsAndUnlocksUsers() {
        when(loginAttemptRepository.findExpiredLockedUserEmails()).thenReturn(java.util.List.of(USER_EMAIL));
        when(userAccessControlApi.unlockAccount(USER_EMAIL)).thenReturn(1);
        when(loginAttemptRepository.resetLockedAccounts()).thenReturn(2);

        service.unlockExpiredAccounts();

        InOrder inOrder = inOrder(userAccessControlApi, loginAttemptRepository);
        inOrder.verify(loginAttemptRepository).findExpiredLockedUserEmails();
        inOrder.verify(userAccessControlApi).unlockAccount(USER_EMAIL);
        inOrder.verify(loginAttemptRepository).resetLockedAccounts();
        verifyNoMoreInteractions(loginAttemptRepository, userAccessControlApi);
    }

    @Test
    @DisplayName("unlockExpiredAccounts propagates database failures for scheduler monitoring")
    void unlockExpiredAccountsPropagatesDatabaseFailures() {
        DataAccessResourceFailureException exception = new DataAccessResourceFailureException("database unavailable");
        when(loginAttemptRepository.resetLockedAccounts()).thenThrow(exception);

        assertThatThrownBy(service::unlockExpiredAccounts).isSameAs(exception);

        verify(loginAttemptRepository).findExpiredLockedUserEmails();
        verify(loginAttemptRepository).resetLockedAccounts();
        verifyNoMoreInteractions(loginAttemptRepository, userAccessControlApi);
    }
}
