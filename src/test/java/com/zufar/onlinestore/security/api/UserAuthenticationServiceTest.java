package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.security.dto.UserAuthenticationRequest;
import com.zufar.onlinestore.security.exception.UserAccountLockedException;
import com.zufar.onlinestore.security.jwt.JwtTokenProvider;
import org.instancio.Instancio;
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
    private String jwtToken = Instancio.create(String.class);

    @Test
    @DisplayName("mock test authenticate, return authentication")
    void mockTestAuthenticate() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal())
                .thenReturn(userDetails);
        when(jwtTokenProvider.generateToken(userDetails))
                .thenReturn(jwtToken);

        userAuthenticationService.authenticate(request);

        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider, times(1))
                .generateToken(userDetails);
        verify(resetLoginAttemptsService, times(1))
                .reset(request.email());
    }

    @Test
    @DisplayName("mock test authenticate throw BadCredentialsException")
    void mockTestAuthenticateThrowBadCredentialsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class,
                () -> userAuthenticationService.authenticate(request));
        verify(loginFailureHandler, times(1))
                .handle(request.email());
    }

    @Test
    @DisplayName("mock test authenticate throw LockedException")
    void mockTestAuthenticateThrowLockedException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new LockedException("User account is locked"));

        assertThrows(UserAccountLockedException.class,
                () -> userAuthenticationService.authenticate(request));
    }
}