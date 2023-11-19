package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.entity.LoginAttemptEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginFailureHandler Tests")
class LoginFailureHandlerTest {

    @Mock
    private FailedLoginAttemptIncrementor failedLoginAttemptIncrementor;

    @Mock
    private UserAccountLocker userAccountLocker;

    @InjectMocks
    private LoginFailureHandler loginFailureHandler;

    private final int MAX_LOGIN_ATTEMPTS = 3;

    private final String userEmail = "user@example.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginFailureHandler, "maxLoginAttempts", MAX_LOGIN_ATTEMPTS);
    }

    @Test
    @DisplayName("Should handle failed login attempt without locking account if attempts are below threshold")
    void shouldHandleFailedLoginWithoutLocking() {
        LoginAttemptEntity loginAttempt = LoginAttemptEntity.builder()
                .id(UUID.randomUUID())
                .userEmail(userEmail)
                .attempts(MAX_LOGIN_ATTEMPTS - 2)
                .isUserLocked(false)
                .lastModified(LocalDateTime.now())
                .build();

        when(failedLoginAttemptIncrementor.increment(userEmail)).thenReturn(loginAttempt);

        loginFailureHandler.handle(userEmail);

        verify(failedLoginAttemptIncrementor, times(1)).increment(userEmail);
        verify(userAccountLocker, never()).lockUserAccount(userEmail);
    }

    @Test
    @DisplayName("Should handle failed login attempt and lock account if attempts reach threshold")
    void shouldHandleFailedLoginAndLockAccount() {
        LoginAttemptEntity loginAttempt = LoginAttemptEntity.builder()
                .id(UUID.randomUUID())
                .userEmail(userEmail)
                .attempts(MAX_LOGIN_ATTEMPTS)
                .isUserLocked(false)
                .lastModified(LocalDateTime.now())
                .build();

        when(failedLoginAttemptIncrementor.increment(userEmail)).thenReturn(loginAttempt);
        doNothing().when(userAccountLocker).lockUserAccount(userEmail);

        loginFailureHandler.handle(userEmail);

        verify(failedLoginAttemptIncrementor, times(1)).increment(userEmail);
        verify(userAccountLocker, times(1)).lockUserAccount(userEmail);
    }
}
