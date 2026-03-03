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
    @DisplayName("Should return JWT token when valid credentials are provided")
    void shouldReturnJwtTokenWhenValidCredentialsProvided() {
        Authentication authentication = mock(Authentication.class);
        String expectedToken = "TestJwtToken";
        String expectedRefreshToken = "TestRefreshToken";
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtTokenProvider.generateToken(userDetails)).thenReturn(expectedToken);
        when(jwtTokenProvider.generateRefreshToken(userDetails)).thenReturn(expectedRefreshToken);

        UserAuthenticationResponse response = userAuthenticationService.authenticate(request);

        assertNotNull(response);
        assertEquals(expectedToken, response.getToken());
        assertEquals(expectedRefreshToken, response.getRefreshToken());
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider).generateToken(userDetails);
        verify(jwtTokenProvider).generateRefreshToken(userDetails);
        verify(resetLoginAttemptsService).reset(request.getEmail());
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when invalid credentials are provided")
    void shouldThrowInvalidCredentialsExceptionWhenInvalidCredentialsProvided() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        InvalidCredentialsException exception = assertThrows(
            InvalidCredentialsException.class,
            () -> userAuthenticationService.authenticate(request)
        );

        assertEquals("Invalid credentials", exception.getMessage());
        verify(loginFailureHandler).handle(request.getEmail());
        verifyNoInteractions(resetLoginAttemptsService);
    }

    @Test
    @DisplayName("Should throw UserAccountLockedException when user account is locked")
    void shouldThrowUserAccountLockedExceptionWhenUserAccountIsLocked() {
        String errorMessage = "User account is locked";
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new LockedException(errorMessage));

        UserAccountLockedException exception = assertThrows(
            UserAccountLockedException.class, 
            () -> userAuthenticationService.authenticate(request)
        );
        
        assertNotNull(exception);
        verifyNoInteractions(loginFailureHandler, resetLoginAttemptsService);
    }
    
    @Test
    @DisplayName("Should authenticate with UserDetails and email")
    void shouldAuthenticateWithUserDetailsAndEmail() {
        String email = "test@example.com";
        String expectedToken = "TestJwtToken";
        String expectedRefreshToken = "TestRefreshToken";
        
        when(jwtTokenProvider.generateToken(userDetails)).thenReturn(expectedToken);
        when(jwtTokenProvider.generateRefreshToken(userDetails)).thenReturn(expectedRefreshToken);

        UserAuthenticationResponse response = userAuthenticationService.authenticate(userDetails, email);

        assertNotNull(response);
        assertEquals(expectedToken, response.getToken());
        assertEquals(expectedRefreshToken, response.getRefreshToken());
        
        verify(jwtTokenProvider).generateToken(userDetails);
        verify(jwtTokenProvider).generateRefreshToken(userDetails);
        verify(resetLoginAttemptsService).reset(email);
    }
}
