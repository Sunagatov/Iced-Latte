package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.security.entity.LoginAttemptEntity;
import com.zufar.onlinestore.security.repository.LoginAttemptRepository;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class ResetLoginAttemptsServiceTest {
    @InjectMocks
    private ResetLoginAttemptsService resetLoginAttemptsService;
    @Mock
    private LoginAttemptRepository loginAttemptRepository;
    @Mock
    private UserAccountLocker userAccountLocker;

    private String userEmail = Instancio.of(String.class)
            .create();
    private Optional<LoginAttemptEntity> loginAttemptEntityForId = Optional.of(Instancio.of(LoginAttemptEntity.class).create());
    private LoginAttemptEntity loginAttemptEntity = Instancio.of(LoginAttemptEntity.class).create();

    @BeforeAll
    static void setUpOnce() {
        mockStatic(LoginAttemptFactory.class);
    }

    @Test
    @DisplayName("Mock test lock user account")
    void testLockUserAccount() {
        when(loginAttemptRepository.findByUserEmail(userEmail))
                .thenReturn(loginAttemptEntityForId);
        when(LoginAttemptFactory.createInitialFailedLoggedAttemptEntity(userEmail))
                .thenReturn(loginAttemptEntity);

        resetLoginAttemptsService.reset(userEmail);

        verify(userAccountLocker, times(1))
                .unlockUserAccount(userEmail);
        verify(loginAttemptRepository, times(1))
                .findByUserEmail(userEmail);
        verify(loginAttemptRepository, times(1))
                .save(loginAttemptEntity);
        assertEquals(loginAttemptEntity.getId(), loginAttemptEntityForId.get().getId());
    }
}