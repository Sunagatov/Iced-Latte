package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.entity.LoginAttemptEntity;
import com.zufar.icedlatte.security.repository.LoginAttemptRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FailedLoginAttemptIncrementor unit tests")
class FailedLoginAttemptIncrementorTest {

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @InjectMocks
    private FailedLoginAttemptIncrementor incrementor;

    private static final String USER_EMAIL = "test@example.com";

    @Nested
    @DisplayName("increment")
    class Increment {

        @Test
        @DisplayName("increments existing login attempt in place")
        void incrementsExistingLoginAttemptInPlace() {
            Instant previousTimestamp = Instant.now().minusSeconds(60);
            LoginAttemptEntity existingAttempt = LoginAttemptEntity.builder()
                    .id(UUID.randomUUID())
                    .userEmail(USER_EMAIL)
                    .attempts(1)
                    .isUserLocked(false)
                    .lastModified(previousTimestamp)
                    .build();
            when(loginAttemptRepository.findByUserEmail(USER_EMAIL)).thenReturn(Optional.of(existingAttempt));
            when(loginAttemptRepository.save(existingAttempt)).thenReturn(existingAttempt);

            LoginAttemptEntity result = incrementor.increment(USER_EMAIL);

            assertThat(result).isSameAs(existingAttempt);
            assertThat(result.getAttempts()).isEqualTo(2);
            assertThat(result.getLastModified()).isAfter(previousTimestamp);
            verify(loginAttemptRepository).findByUserEmail(USER_EMAIL);
            verify(loginAttemptRepository).save(existingAttempt);
            verifyNoMoreInteractions(loginAttemptRepository);
        }

        @Test
        @DisplayName("creates new unlocked login attempt when record does not exist")
        void createsNewUnlockedLoginAttemptWhenRecordDoesNotExist() {
            ArgumentCaptor<LoginAttemptEntity> captor = ArgumentCaptor.forClass(LoginAttemptEntity.class);
            when(loginAttemptRepository.findByUserEmail(USER_EMAIL)).thenReturn(Optional.empty());
            when(loginAttemptRepository.save(any(LoginAttemptEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            LoginAttemptEntity result = incrementor.increment(USER_EMAIL);

            verify(loginAttemptRepository).findByUserEmail(USER_EMAIL);
            verify(loginAttemptRepository).save(captor.capture());
            verifyNoMoreInteractions(loginAttemptRepository);

            LoginAttemptEntity saved = captor.getValue();
            assertThat(result).isSameAs(saved);
            assertThat(saved.getUserEmail()).isEqualTo(USER_EMAIL);
            assertThat(saved.getAttempts()).isEqualTo(1);
            assertThat(saved.getIsUserLocked()).isFalse();
            assertThat(saved.getLastModified()).isNotNull();
        }
    }
}
