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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResetLoginAttemptsService Tests")
class ResetLoginAttemptsServiceTest {

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private UserAccountLocker userAccountLocker;

    @InjectMocks
    private ResetLoginAttemptsService resetLoginAttemptsService;

    private final String userEmail = "user@example.com";

    @Test
    @DisplayName("Should reset login attempts and unlock account for locked user")
    void shouldResetLoginAttemptsForExistingUser() {
        LoginAttemptEntity existingLoginAttempt = LoginAttemptEntity.builder()
                .id(UUID.randomUUID())
                .userEmail(userEmail)
                .attempts(3)
                .isUserLocked(true)
                .lastModified(Instant.now())
                .build();

        when(loginAttemptRepository.findByUserEmail(userEmail)).thenReturn(Optional.of(existingLoginAttempt));

        resetLoginAttemptsService.reset(userEmail);

        verify(userAccountLocker, times(1)).unlockUserAccount(userEmail);
        verify(loginAttemptRepository, never()).save(any());
        assertEquals(0, existingLoginAttempt.getAttempts());
        assertFalse(existingLoginAttempt.getIsUserLocked());
    }

    @Test
    @DisplayName("Should not perform reset for non-existing user")
    void shouldNotResetLoginAttemptsForNonExistingUser() {
        when(loginAttemptRepository.findByUserEmail(userEmail)).thenReturn(Optional.empty());

        resetLoginAttemptsService.reset(userEmail);

        verify(userAccountLocker, never()).unlockUserAccount(userEmail);
        verify(loginAttemptRepository, never()).save(any());
    }
}
