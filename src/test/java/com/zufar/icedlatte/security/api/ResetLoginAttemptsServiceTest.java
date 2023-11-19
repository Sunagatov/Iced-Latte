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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@DisplayName("ResetLoginAttemptsService Tests")
class ResetLoginAttemptsServiceTest {

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private UserAccountLocker userAccountLocker;

    @Mock
    private LoginAttemptFactory loginAttemptFactory;

    @InjectMocks
    private ResetLoginAttemptsService resetLoginAttemptsService;

    private final String userEmail = "user@example.com";

    @Test
    @DisplayName("Should reset login attempts for existing user")
    void shouldResetLoginAttemptsForExistingUser() {
        LoginAttemptEntity existingLoginAttempt = LoginAttemptEntity.builder()
                .id(UUID.randomUUID())
                .userEmail(userEmail)
                .attempts(5)
                .isUserLocked(true)
                .lastModified(LocalDateTime.now())
                .build();

        LoginAttemptEntity resetLoginAttempt = LoginAttemptEntity.builder()
                .id(existingLoginAttempt.getId())
                .userEmail(userEmail)
                .attempts(0)
                .isUserLocked(false)
                .lastModified(LocalDateTime.now())
                .build();

        when(loginAttemptRepository.findByUserEmail(userEmail)).thenReturn(Optional.of(existingLoginAttempt));
        when(loginAttemptFactory.createInitialFailedLoggedAttemptEntity(userEmail)).thenReturn(resetLoginAttempt);

        resetLoginAttemptsService.reset(userEmail);

        verify(userAccountLocker, times(1)).unlockUserAccount(userEmail);
        verify(loginAttemptRepository, times(1)).findByUserEmail(userEmail);
        verify(loginAttemptFactory, times(1)).createInitialFailedLoggedAttemptEntity(userEmail);
        verify(loginAttemptRepository, times(1)).save(resetLoginAttempt);
    }

    @Test
    @DisplayName("Should not perform reset for non-existing user")
    void shouldNotResetLoginAttemptsForNonExistingUser() {
        when(loginAttemptRepository.findByUserEmail(userEmail)).thenReturn(Optional.empty());

        resetLoginAttemptsService.reset(userEmail);

        verify(userAccountLocker, times(1)).unlockUserAccount(userEmail);
        verify(loginAttemptRepository, times(1)).findByUserEmail(userEmail);
        verify(loginAttemptFactory, never()).createInitialFailedLoggedAttemptEntity(userEmail);
        verify(loginAttemptRepository, never()).save(any(LoginAttemptEntity.class));
    }
}
