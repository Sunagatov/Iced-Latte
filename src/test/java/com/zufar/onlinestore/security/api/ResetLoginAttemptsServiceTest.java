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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResetLoginAttemptsServiceTest {

    @InjectMocks
    private ResetLoginAttemptsService resetLoginAttemptsService;

    @Mock
    private LoginAttemptRepository loginAttemptRepository;

    @Mock
    private UserAccountLocker userAccountLocker;

    private String userEmail = "TestEmail";
    private Optional<LoginAttemptEntity> loginAttemptEntityForId = Optional.of(Instancio.create(LoginAttemptEntity.class));
    private LoginAttemptEntity loginAttemptEntity = Instancio.create(LoginAttemptEntity.class);

    private static MockedStatic<LoginAttemptFactory> mockedSettings;

    @BeforeAll
    static void setUpOnce() {
        mockedSettings = mockStatic(LoginAttemptFactory.class);
    }

    @AfterAll
    static void tearDownOnce() {
        mockedSettings.close();
    }

    @Test
    @DisplayName("Should reset login attempts and unlock user account")
    void shouldResetLoginAttemptsAndUnlockUserAccount() {
        when(loginAttemptRepository.findByUserEmail(userEmail)).thenReturn(loginAttemptEntityForId);
        mockedSettings.when(()->LoginAttemptFactory.createInitialFailedLoggedAttemptEntity(userEmail)).thenReturn(loginAttemptEntity);

        assertDoesNotThrow(()->resetLoginAttemptsService.reset(userEmail));

        verify(userAccountLocker, times(1)).unlockUserAccount(userEmail);
        verify(loginAttemptRepository, times(1)).findByUserEmail(userEmail);
        verify(loginAttemptRepository, times(1)).save(loginAttemptEntity);
        assertEquals(loginAttemptEntity.getId(), loginAttemptEntityForId.get().getId());
    }
}