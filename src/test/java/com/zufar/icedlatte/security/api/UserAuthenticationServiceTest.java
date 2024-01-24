package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.security.dto.UserAuthenticationRequest;
import com.zufar.icedlatte.security.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.security.exception.UserAccountLockedException;
import com.zufar.icedlatte.security.jwt.JwtTokenProvider;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private final UserAuthenticationRequest request = Instancio.create(UserAuthenticationRequest.class);
    private final UserDetails userDetails = Instancio.create(UserDetails.class);

    @Test
    @DisplayName("Should Return JWT Token When Valid Credentials Are Provided")
    void shouldReturnJwtTokenWhenValidCredentialsProvided() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        String jwtToken = "TestJwtToken";
        when(jwtTokenProvider.generateToken(userDetails)).thenReturn(jwtToken);

        UserAuthenticationResponse response = userAuthenticationService.authenticate(request);

        assertEquals(new UserAuthenticationResponse(jwtToken,null), response);
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider, times(1)).generateToken(userDetails);
        verify(resetLoginAttemptsService, times(1)).reset(request.email());
    }

    @Test
    @DisplayName("Should Throw BadCredentialsException When Invalid Credentials Are Provided")
    void shouldThrowBadCredentialsExceptionWhenInvalidCredentialsProvided() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(BadCredentialsException.class, () -> userAuthenticationService.authenticate(request));

        verify(loginFailureHandler, times(1)).handle(request.email());
    }

    @Test
    @DisplayName("Should Throw UserAccountLockedException When User Account Is Locked")
    void shouldThrowUserAccountLockedExceptionWhenUserAccountIsLocked() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new LockedException("User account is locked"));

        assertThrows(UserAccountLockedException.class, () -> userAuthenticationService.authenticate(request));
    }
}
