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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

    private String userEmail = "TestEmail";
    private LoginAttemptEntity loginAttempt = Instancio.create(LoginAttemptEntity.class);
    private int maxLoginAttempts = 2;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginFailureHandler, "maxLoginAttempts", maxLoginAttempts);
    }

    @Test
    @DisplayName("Should handle login failure without locking user account")
    void givenMaxLoginAttemptsReachedWhenHandleLoginFailureThenLockUserAccount() {
        loginAttempt.setAttempts(maxLoginAttempts - 1);
        when(failedLoginAttemptIncrementor.increment(userEmail)).thenReturn(loginAttempt);

        assertDoesNotThrow(() -> loginFailureHandler.handle(userEmail));

        verify(userAccountLocker, times(0)).lockUserAccount(userEmail);
        verify(failedLoginAttemptIncrementor, times(1)).increment(userEmail);
    }

    @Test
    @DisplayName("Should handle login failure with locking user account")
    void givenMaxLoginAttemptsNotReachedWhenHandleLoginFailureThenNotLockUserAccount() {
        loginAttempt.setAttempts(maxLoginAttempts + 1);
        when(failedLoginAttemptIncrementor.increment(userEmail)).thenReturn(loginAttempt);

        assertDoesNotThrow(() -> loginFailureHandler.handle(userEmail));

        verify(userAccountLocker, times(1)).lockUserAccount(userEmail);
        verify(failedLoginAttemptIncrementor, times(1)).increment(userEmail);
    }
}