package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.security.entity.LoginAttemptEntity;
import com.zufar.onlinestore.security.repository.LoginAttemptRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class FailedLoginAttemptIncrementorTest {
    @InjectMocks
    private FailedLoginAttemptIncrementor failedLoginAttemptIncrementor;
    @Mock
    private LoginAttemptRepository loginAttemptRepository;
    private String userEmail = Instancio.of(String.class)
            .create();
    private LoginAttemptEntity loginAttemptEntity = Instancio.of(LoginAttemptEntity.class)
            .create();
    private LoginAttemptEntity loginAttemptEntityNoAttempt = Instancio.of(LoginAttemptEntity.class)
            .create();
    private LocalDateTime timeUnderRunningMethod = LocalDateTime.now();

    @BeforeAll
    static void setUpOne() {
        mockStatic(LoginAttemptFactory.class);
    }

    @Test
    public void testIncrementLoginSuccess() {
        when(loginAttemptRepository.findByUserEmail(userEmail))
                .thenReturn(Optional.ofNullable(loginAttemptEntity));
        int attempts = loginAttemptEntity.getAttempts();

        failedLoginAttemptIncrementor.increment(userEmail);

        verify(loginAttemptRepository, times(1))
                .findByUserEmail(userEmail);
        assertEquals(attempts + 1, loginAttemptEntity.getAttempts());
        assertTrue(loginAttemptEntity.getLastModified().isAfter(timeUnderRunningMethod));
        verify(loginAttemptRepository, times(1))
                .save(loginAttemptEntity);
    }

    @Test
    public void testIncrementNoLoginAttempt() {
        when(loginAttemptRepository.findByUserEmail(userEmail))
                .thenReturn(Optional.empty());
        when(LoginAttemptFactory.createInitialFailedLoggedAttemptEntity(userEmail))
                .thenReturn(loginAttemptEntityNoAttempt);
        int attempts = loginAttemptEntityNoAttempt.getAttempts();

        failedLoginAttemptIncrementor.increment(userEmail);
        
        verify(loginAttemptRepository, times(1))
                .findByUserEmail(userEmail);
        assertEquals(attempts + 1, loginAttemptEntityNoAttempt.getAttempts());
        assertTrue(loginAttemptEntityNoAttempt.getLastModified().isAfter(timeUnderRunningMethod));
        verify(loginAttemptRepository, times(1))
                .save(loginAttemptEntityNoAttempt);
    }
}