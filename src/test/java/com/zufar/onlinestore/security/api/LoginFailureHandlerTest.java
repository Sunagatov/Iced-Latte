package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.security.entity.LoginAttemptEntity;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginFailureHandlerTest {
    @InjectMocks
    private LoginFailureHandler loginFailureHandler;
    @Mock
    private FailedLoginAttemptIncrementor failedLoginAttemptIncrementor;
    @Mock
    private UserAccountLocker userAccountLocker;

    private String userEmail = Instancio.create(String.class);
    private LoginAttemptEntity loginAttempt = Instancio.create(LoginAttemptEntity.class);
    private int maxLoginAttempts = 2;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginFailureHandler, "maxLoginAttempts", maxLoginAttempts);
    }

    @Test
    @DisplayName("test handle without lock user account")
    public void testHandleLessThenMax() {
        loginAttempt.setAttempts(maxLoginAttempts - 1);
        when(failedLoginAttemptIncrementor.increment(userEmail))
                .thenReturn(loginAttempt);

        loginFailureHandler.handle(userEmail);

        verify(userAccountLocker, times(0))
                .lockUserAccount(userEmail);
        verify(failedLoginAttemptIncrementor, times(1))
                .increment(userEmail);
    }

    @Test
    @DisplayName("test handle with lock user account")
    public void testHandleMoreThenMax() {
        loginAttempt.setAttempts(maxLoginAttempts + 1);
        when(failedLoginAttemptIncrementor.increment(userEmail))
                .thenReturn(loginAttempt);

        loginFailureHandler.handle(userEmail);

        verify(userAccountLocker, times(1))
                .lockUserAccount(userEmail);
        verify(failedLoginAttemptIncrementor, times(1))
                .increment(userEmail);
    }
}