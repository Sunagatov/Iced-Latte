package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.security.entity.LoginAttemptEntity;
import com.zufar.onlinestore.security.repository.LoginAttemptRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

    private String userEmail = "TestEmail";
    private LoginAttemptEntity loginAttemptEntity = Instancio.of(LoginAttemptEntity.class).create();
    private LoginAttemptEntity loginAttemptEntityNoAttempt = Instancio.of(LoginAttemptEntity.class).create();
    private LocalDateTime timeBeforeRunningMethod = LocalDateTime.now().minusSeconds(1);

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
    @DisplayName("Increment When Existing Login Attempt")
    void givenExistingLoginAttemptWhenIncrementingLoginAttemptThenIncrementAttemptsAndSave() {
        when(loginAttemptRepository.findByUserEmail(userEmail)).thenReturn(Optional.ofNullable(loginAttemptEntity));
        when(loginAttemptRepository.save(loginAttemptEntity)).thenReturn(loginAttemptEntity);
        int attempts = loginAttemptEntity.getAttempts();

        LoginAttemptEntity result = failedLoginAttemptIncrementor.increment(userEmail);

        assertEquals(result, loginAttemptEntity);
        verify(loginAttemptRepository, times(1)).findByUserEmail(userEmail);
        assertEquals(attempts + 1, loginAttemptEntity.getAttempts());
        assertTrue(loginAttemptEntity.getLastModified().isAfter(timeBeforeRunningMethod));
        verify(loginAttemptRepository, times(1)).save(loginAttemptEntity);
    }

    @Test
    @DisplayName("Increment When No Previous Login Attempt")
    void givenNoPreviousLoginAttemptWhenIncrementingLoginAttemptThenCreateAndSaveNewLoginAttempt() {
        when(loginAttemptRepository.findByUserEmail(userEmail)).thenReturn(Optional.empty());
        mockStatic.when(() -> LoginAttemptFactory.createInitialFailedLoggedAttemptEntity(userEmail)).thenReturn(loginAttemptEntityNoAttempt);
        when(loginAttemptRepository.save(loginAttemptEntityNoAttempt)).thenReturn(loginAttemptEntityNoAttempt);
        int attempts = loginAttemptEntityNoAttempt.getAttempts();

        LoginAttemptEntity result = failedLoginAttemptIncrementor.increment(userEmail);

        assertEquals(result, loginAttemptEntityNoAttempt);
        verify(loginAttemptRepository, times(1)).findByUserEmail(userEmail);
        assertEquals(attempts + 1, loginAttemptEntityNoAttempt.getAttempts());
        assertTrue(loginAttemptEntityNoAttempt.getLastModified().isAfter(timeBeforeRunningMethod));
        verify(loginAttemptRepository, times(1)).save(loginAttemptEntityNoAttempt);
    }
}