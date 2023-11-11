package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.security.dto.UserAuthenticationRequest;
import com.zufar.onlinestore.security.dto.UserAuthenticationResponse;
import com.zufar.onlinestore.security.exception.UserAccountLockedException;
import com.zufar.onlinestore.security.jwt.JwtTokenProvider;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
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

    @Mock
    private Authentication authentication;

    private UserAuthenticationRequest request = Instancio.create(UserAuthenticationRequest.class);
    private UserDetails userDetails = Instancio.create(UserDetails.class);
    private String jwtToken = "TestJwtToken";

    @Test
    @DisplayName("Authenticate when valid credentials provided, then return authentication")
    public void authenticateWhenValidCredentialsProvidedThenReturnAuthentication() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtTokenProvider.generateToken(userDetails)).thenReturn(jwtToken);

        UserAuthenticationResponse response = userAuthenticationService.authenticate(request);

        assertEquals(response, new UserAuthenticationResponse(jwtToken));
        ArgumentCaptor<UsernamePasswordAuthenticationToken> authenticationTokenArgumentCaptor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager, times(1)).authenticate(authenticationTokenArgumentCaptor.capture());
        verify(jwtTokenProvider, times(1)).generateToken(userDetails);
        verify(resetLoginAttemptsService, times(1)).reset(request.email());
    }

    @Test
    @DisplayName("Authenticate when invalid credentials provided, then throw BadCredentialsException")
    void authenticateWhenInvalidCredentialsProvidedThenThrowBadCredentialsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class, () -> userAuthenticationService.authenticate(request));
        verify(loginFailureHandler, times(1)).handle(request.email());
    }

    @Test
    @DisplayName("Authenticate when user account is locked, then throw UserAccountLockedException")
    void authenticateWhenUserAccountIsLockedThenThrowUserAccountLockedException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(new LockedException("User account is locked"));

        assertThrows(UserAccountLockedException.class, () -> userAuthenticationService.authenticate(request));
    }
}