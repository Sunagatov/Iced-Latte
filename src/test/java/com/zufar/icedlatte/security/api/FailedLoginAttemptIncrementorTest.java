package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.entity.LoginAttemptEntity;
import com.zufar.icedlatte.security.repository.LoginAttemptRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FailedLoginAttemptIncrementor Tests")
class FailedLoginAttemptIncrementorTest {

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private LoginAttemptFactory loginAttemptFactory;

    @InjectMocks
    private FailedLoginAttemptIncrementor failedLoginAttemptIncrementor;

    private final String userEmail = "test@example.com";

    @Test
    @DisplayName("Should correctly increment an existing login attempt")
    void shouldCorrectlyIncrementExistingLoginAttempt() {
        int initialAttempts = 1;
        LocalDateTime initialLastModified = LocalDateTime.now();
        UUID attemptId = UUID.randomUUID();
        LoginAttemptEntity existingLoginAttempt = LoginAttemptEntity.builder()
                .id(attemptId)
                .userEmail(userEmail)
                .attempts(initialAttempts)
                .isUserLocked(false)
                .lastModified(initialLastModified)
                .build();

        LoginAttemptEntity expectedUpdatedLoginAttempt = LoginAttemptEntity.builder()
                .id(attemptId)
                .userEmail(userEmail)
                .attempts(initialAttempts + 1)
                .isUserLocked(false)
                .lastModified(initialLastModified.plusSeconds(2))
                .build();

        when(loginAttemptRepository.findByUserEmail(userEmail)).thenReturn(Optional.of(existingLoginAttempt));
        when(loginAttemptRepository.save(existingLoginAttempt)).thenReturn(expectedUpdatedLoginAttempt);

        LoginAttemptEntity actualUpdatedLoginAttempt = failedLoginAttemptIncrementor.increment(userEmail);

        assertNotNull(actualUpdatedLoginAttempt, "Updated login attempt should not be null");
        assertEquals(expectedUpdatedLoginAttempt.getAttempts(), actualUpdatedLoginAttempt.getAttempts(), "The attempts should be incremented");
        assertNotEquals(initialLastModified, actualUpdatedLoginAttempt.getLastModified(), "The last modified time should be updated");
        assertTrue(actualUpdatedLoginAttempt.getLastModified().isAfter(initialLastModified), "The last modified time should be updated and be after the initial time");

        verify(loginAttemptRepository, (times(1))).findByUserEmail(userEmail);
        verify(loginAttemptRepository, (times(1))).save(existingLoginAttempt);
        verify(loginAttemptFactory, never()).createInitialFailedLoggedAttemptEntity(userEmail);
    }

    @Test
    @DisplayName("Should create new login attempt if it does not exist")
    void shouldCreateNewLoginAttemptIfNotExists() {
        LocalDateTime initialLastModified = LocalDateTime.now();
        LoginAttemptEntity newLoginAttempt = LoginAttemptEntity.builder()
                .id(null)
                .userEmail(userEmail)
                .attempts(0)
                .isUserLocked(false)
                .lastModified(initialLastModified)
                .build();

        LoginAttemptEntity expectedSavedLoginAttempt = LoginAttemptEntity.builder()
                .id(UUID.randomUUID())
                .userEmail(userEmail)
                .attempts(1)
                .isUserLocked(false)
                .lastModified(initialLastModified)
                .build();

        when(loginAttemptRepository.findByUserEmail(userEmail)).thenReturn(Optional.empty());
        when(loginAttemptFactory.createInitialFailedLoggedAttemptEntity(userEmail)).thenReturn(newLoginAttempt);
        when(loginAttemptRepository.save(newLoginAttempt)).thenReturn(expectedSavedLoginAttempt);

        LoginAttemptEntity actualSavedLoginAttempt = failedLoginAttemptIncrementor.increment(userEmail);

        assertNotNull(actualSavedLoginAttempt, "A new login attempt should be created and not be null");
        assertEquals(expectedSavedLoginAttempt.getAttempts(), actualSavedLoginAttempt.getAttempts(), "The attempts should be incremented in the new login attempt");
        assertEquals(expectedSavedLoginAttempt.getLastModified(), actualSavedLoginAttempt.getLastModified(), "The last modified time should be set correctly in the new login attempt");
        assertNotNull(actualSavedLoginAttempt.getId(), "The ID should be set correctly in the new login attempt");

        verify(loginAttemptRepository).findByUserEmail(userEmail);
        verify(loginAttemptFactory).createInitialFailedLoggedAttemptEntity(userEmail);
        verify(loginAttemptRepository).save(newLoginAttempt);
    }
}

