package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.security.entity.LoginAttemptEntity;
import com.zufar.onlinestore.security.repository.LoginAttemptRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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

    private static MockedStatic<LoginAttemptFactory> mockStatic;

    @BeforeAll
    static void setUpOne() {
        mockStatic = mockStatic(LoginAttemptFactory.class);
    }

    @AfterAll
    static void tearDownOne() {
        mockStatic.close();
    }

    @Test
    @DisplayName("Incrementing Login Success")
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
    @DisplayName("Incrementing When No Previous Login Attempt")
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