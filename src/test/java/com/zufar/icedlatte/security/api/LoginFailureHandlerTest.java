package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.entity.LoginAttemptEntity;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
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

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LoginFailureHandler loginFailureHandler;

    @Value("${security.max-login-attempts}")
    private int maxLoginAttempts;

    private final String userEmail = "user@example.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginFailureHandler, "maxLoginAttempts", maxLoginAttempts);
        when(userRepository.findByEmail(userEmail)).thenReturn(java.util.Optional.of(new UserEntity()));
    }

    @Test
    @DisplayName("Should handle failed login attempt without locking account if attempts are below threshold")
    void shouldHandleFailedLoginWithoutLocking() {
        LoginAttemptEntity loginAttempt = LoginAttemptEntity.builder()
                .id(UUID.randomUUID())
                .userEmail(userEmail)
                .attempts(maxLoginAttempts - 2)
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
                .attempts(maxLoginAttempts)
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
