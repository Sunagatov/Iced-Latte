package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.entity.LoginAttemptEntity;
import com.zufar.icedlatte.security.repository.LoginAttemptRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FailedLoginAttemptIncrementor Tests")
class FailedLoginAttemptIncrementorTest {

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @InjectMocks
    private FailedLoginAttemptIncrementor failedLoginAttemptIncrementor;

    private final String userEmail = "test@example.com";

    @Test
    @DisplayName("Should correctly increment an existing login attempt")
    void shouldCorrectlyIncrementExistingLoginAttempt() {
        int initialAttempts = 1;
        UUID attemptId = UUID.randomUUID();
        LoginAttemptEntity existingLoginAttempt = LoginAttemptEntity.builder()
                .id(attemptId)
                .userEmail(userEmail)
                .attempts(initialAttempts)
                .isUserLocked(false)
                .lastModified(Instant.now())
                .build();

        when(loginAttemptRepository.findByUserEmail(userEmail)).thenReturn(Optional.of(existingLoginAttempt));
        when(loginAttemptRepository.save(existingLoginAttempt)).thenReturn(existingLoginAttempt);

        LoginAttemptEntity result = failedLoginAttemptIncrementor.increment(userEmail);

        assertNotNull(result);
        assertEquals(initialAttempts + 1, result.getAttempts());
        verify(loginAttemptRepository).findByUserEmail(userEmail);
        verify(loginAttemptRepository).save(existingLoginAttempt);
    }

    @Test
    @DisplayName("Should create new login attempt if it does not exist")
    void shouldCreateNewLoginAttemptIfNotExists() {
        LoginAttemptEntity saved = LoginAttemptEntity.builder()
                .id(UUID.randomUUID())
                .userEmail(userEmail)
                .attempts(1)
                .isUserLocked(false)
                .lastModified(Instant.now())
                .build();

        when(loginAttemptRepository.findByUserEmail(userEmail)).thenReturn(Optional.empty());
        when(loginAttemptRepository.save(any(LoginAttemptEntity.class))).thenReturn(saved);

        LoginAttemptEntity result = failedLoginAttemptIncrementor.increment(userEmail);

        assertNotNull(result);
        assertEquals(1, result.getAttempts());
        verify(loginAttemptRepository).findByUserEmail(userEmail);
        verify(loginAttemptRepository).save(any(LoginAttemptEntity.class));
    }
}
