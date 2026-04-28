package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.entity.LoginAttemptEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginFailureHandler unit tests")
class LoginFailureHandlerTest {

    @Mock
    private FailedLoginAttemptIncrementor failedLoginAttemptIncrementor;

    @Mock
    private UserAccountLocker userAccountLocker;

    @InjectMocks
    private LoginFailureHandler handler;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final String USER_EMAIL = "user@example.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "maxLoginAttempts", MAX_LOGIN_ATTEMPTS);
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        @DisplayName("does not lock when attempts stay below threshold")
        void doesNotLockWhenAttemptsStayBelowThreshold() {
            when(failedLoginAttemptIncrementor.increment(USER_EMAIL)).thenReturn(loginAttempt(4));

            handler.handle(USER_EMAIL);

            verify(failedLoginAttemptIncrementor).increment(USER_EMAIL);
            verify(userAccountLocker, never()).lockUserAccount(USER_EMAIL);
            verifyNoMoreInteractions(failedLoginAttemptIncrementor, userAccountLocker);
        }

        @Test
        @DisplayName("locks account when attempts reach threshold")
        void locksWhenAttemptsReachThreshold() {
            when(failedLoginAttemptIncrementor.increment(USER_EMAIL)).thenReturn(loginAttempt(5));

            handler.handle(USER_EMAIL);

            verify(failedLoginAttemptIncrementor).increment(USER_EMAIL);
            verify(userAccountLocker).lockUserAccount(USER_EMAIL);
            verifyNoMoreInteractions(failedLoginAttemptIncrementor, userAccountLocker);
        }

        @Test
        @DisplayName("locks account when attempts exceed threshold")
        void locksWhenAttemptsExceedThreshold() {
            when(failedLoginAttemptIncrementor.increment(USER_EMAIL)).thenReturn(loginAttempt(8));

            handler.handle(USER_EMAIL);

            verify(failedLoginAttemptIncrementor).increment(USER_EMAIL);
            verify(userAccountLocker).lockUserAccount(USER_EMAIL);
            verifyNoMoreInteractions(failedLoginAttemptIncrementor, userAccountLocker);
        }
    }

    private static LoginAttemptEntity loginAttempt(int attempts) {
        return LoginAttemptEntity.builder()
                .userEmail(USER_EMAIL)
                .attempts(attempts)
                .isUserLocked(false)
                .build();
    }
}
