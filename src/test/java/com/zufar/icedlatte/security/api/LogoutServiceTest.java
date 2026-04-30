package com.zufar.icedlatte.security.api;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogoutService unit tests")
class LogoutServiceTest {

    @Mock private TokenRevocationService tokenRevocationService;
    @Mock private AuthSessionService authSessionService;
    @Mock private HttpServletRequest request;

    @InjectMocks private LogoutService service;

    @Test
    @DisplayName("logout delegates token revocation")
    void logoutDelegatesTokenRevocation() {
        service.logout("refresh-token", request);

        verify(tokenRevocationService).revokeTokens("refresh-token", request);
    }

    @Test
    @DisplayName("logoutAll revokes all sessions for the user")
    void logoutAllRevokesAllSessionsForUser() {
        UUID userId = UUID.randomUUID();

        service.logoutAll(userId);

        verify(authSessionService).revokeAllForUser(userId);
    }
}
