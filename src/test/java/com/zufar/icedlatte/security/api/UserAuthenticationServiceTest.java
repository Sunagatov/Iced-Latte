package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.openapi.dto.UserAuthenticationRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.exception.InvalidCredentialsException;
import com.zufar.icedlatte.security.exception.UserAccountLockedException;
import com.zufar.icedlatte.security.jwt.JwtTokenProvider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAuthenticationService Tests")
class UserAuthenticationServiceTest {

    @InjectMocks
    private UserAuthenticationService userAuthenticationService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private LoginFailureHandler loginFailureHandler;

    @Mock
    private ResetLoginAttemptsService resetLoginAttemptsService;

    private final UserAuthenticationRequest request = mock(UserAuthenticationRequest.class);
    private final UserDetails userDetails = mock(UserDetails.class);

    @Test
    @DisplayName("Should return UserDetails when valid credentials are provided")
    void shouldReturnUserDetailsWhenValidCredentialsProvided() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        UserDetails result = userAuthenticationService.verifyCredentials(request);

        assertSame(userDetails, result);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when invalid credentials are provided")
    void shouldThrowInvalidCredentialsExceptionWhenInvalidCredentialsProvided() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(InvalidCredentialsException.class, () -> userAuthenticationService.verifyCredentials(request));

        verify(loginFailureHandler).handle(request.getEmail());
        verifyNoInteractions(resetLoginAttemptsService);
    }

    @Test
    @DisplayName("Should throw UserAccountLockedException when user account is locked")
    void shouldThrowUserAccountLockedExceptionWhenUserAccountIsLocked() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new LockedException("User account is locked"));

        assertThrows(UserAccountLockedException.class, () -> userAuthenticationService.verifyCredentials(request));

        verifyNoInteractions(loginFailureHandler, resetLoginAttemptsService);
    }

    @Test
    @DisplayName("Should build session-bound token pair")
    void shouldBuildSessionBoundTokenPair() {
        String email = "test@example.com";
        UUID sessionId = UUID.randomUUID();
        String refreshToken = "raw-refresh-token";
        String expectedAccessToken = "access-token";

        when(jwtTokenProvider.generateToken(userDetails, sessionId)).thenReturn(expectedAccessToken);

        UserAuthenticationResponse response = userAuthenticationService.buildTokenPair(userDetails, email, sessionId, refreshToken);

        assertNotNull(response);
        assertEquals(expectedAccessToken, response.getToken());
        assertEquals(refreshToken, response.getRefreshToken());
        verify(resetLoginAttemptsService).reset(email);
    }
}
