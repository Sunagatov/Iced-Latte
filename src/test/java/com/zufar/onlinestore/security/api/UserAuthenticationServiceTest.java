package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.security.dto.UserAuthenticationRequest;
import com.zufar.onlinestore.security.jwt.JwtTokenProvider;
import com.zufar.onlinestore.user.api.UserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class UserAuthenticationServiceTest {

    @InjectMocks
    private UserAuthenticationService userAuthenticationService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserApi userApi;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private Authentication authentication;
    @Mock
    private UserDetails userDetails;
    private String hashPassword;
    private UserAuthenticationRequest request;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        request = new UserAuthenticationRequest("email", "password");
        hashPassword = "password";
        jwtToken = "jwtToken";

        Mockito.when(userApi.getPasswordByEmail(any(String.class)))
                .thenReturn(hashPassword);

    }

    @Test
    @DisplayName("mock test authenticate")
    void authenticate() {
        Mockito.when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        Mockito.when(authentication.getPrincipal())
                .thenReturn(userDetails);
        Mockito.when(jwtTokenProvider.generateToken(userDetails))
                .thenReturn(jwtToken);

        Mockito.when(passwordEncoder.matches(any(String.class), any(String.class)))
                .thenReturn(true);

        userAuthenticationService.authenticate(request);

        Mockito.verify(authentication, Mockito.times(1))
                .getPrincipal();
        Mockito.verify(jwtTokenProvider, Mockito.times(1))
                .generateToken(userDetails);
    }

    @Test
    @DisplayName("mock test getAuthenticate (true)")
    void getAuthenticateTrue() {
        Mockito.when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        Mockito.when(authentication.getPrincipal())
                .thenReturn(userDetails);
        Mockito.when(jwtTokenProvider.generateToken(userDetails))
                .thenReturn(jwtToken);

        Mockito.when(passwordEncoder.matches(any(String.class), any(String.class)))
                .thenReturn(true);

        userAuthenticationService.authenticate(request);

        Mockito.verify(userApi, Mockito.times(1))
                .getPasswordByEmail(request.email());
        Mockito.verify(passwordEncoder, Mockito.times(1))
                .matches(any(String.class), any(String.class));
        Mockito.verify(authenticationManager, Mockito.times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("mock test getAuthenticate (false)")
    void getAuthenticateFalse() {
        assertThrows(BadCredentialsException.class, () -> {
            userAuthenticationService.authenticate(request);
        });
    }
}