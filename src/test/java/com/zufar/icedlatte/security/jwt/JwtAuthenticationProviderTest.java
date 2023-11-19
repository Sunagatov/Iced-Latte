package com.zufar.icedlatte.security.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationProvider Tests")
class JwtAuthenticationProviderTest {

    @Mock
    private JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor;

    @Mock
    private JwtClaimExtractor jwtClaimExtractor;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtBlacklistValidator jwtBlacklistValidator;

    @InjectMocks
    private JwtAuthenticationProvider jwtAuthenticationProvider;

    @Test
    @DisplayName("Should successfully authenticate user")
    void shouldSuccessfullyAuthenticateUser() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        UserDetails userDetails = new User("test@example.com", "password", Collections.emptyList());
        String jwtToken = "mockJwtToken";
        String userEmail = "test@example.com";

        when(jwtTokenFromAuthHeaderExtractor.extract(httpRequest)).thenReturn(jwtToken);
        when(jwtClaimExtractor.extractEmail(jwtToken)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(userDetails);

        var authenticationToken = jwtAuthenticationProvider.get(httpRequest);

        assertNotNull(authenticationToken);
        assertEquals(userDetails, authenticationToken.getPrincipal());
        verify(jwtBlacklistValidator).validate(jwtToken);
        verify(jwtClaimExtractor).extractExpiration(jwtToken);
    }
}
