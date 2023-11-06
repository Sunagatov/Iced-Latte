package com.zufar.onlinestore.security.jwt;

import jakarta.servlet.http.HttpServletRequest;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import java.util.Collections;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class JwtAuthenticationProviderTest {
    @InjectMocks
    private JwtAuthenticationProvider jwtAuthenticationProvider;
    @Mock
    private JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor;
    @Mock
    private JwtClaimExtractor jwtClaimExtractor;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private JwtBlacklistValidator jwtBlacklistValidator;
    @Mock
    private HttpServletRequest httpRequest;
    private String jwtToken = Instancio.create(String.class);
    private String userEmail = Instancio.create(String.class);
    @Mock
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        when(jwtTokenFromAuthHeaderExtractor.extract(httpRequest))
                .thenReturn(jwtToken);
        when(jwtClaimExtractor.extractEmail(jwtToken))
                .thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail))
                .thenReturn(userDetails);
        when(userDetails.getAuthorities())
                .thenReturn(Collections.emptyList());
    }

    @Test
    void mockTestGet() {
        MockedConstruction<WebAuthenticationDetailsSource> webAuthDetailsSourceConstruction =
                mockConstruction(WebAuthenticationDetailsSource.class);

        MockedConstruction<UsernamePasswordAuthenticationToken> authTokenConstruction =
                mockConstruction(UsernamePasswordAuthenticationToken.class);

        Authentication result = jwtAuthenticationProvider.get(httpRequest);

        assertNotNull(result);
        assertEquals(userDetails, result.getPrincipal());

        verify(jwtTokenFromAuthHeaderExtractor, times(1))
                .extract(httpRequest);
        verify(jwtBlacklistValidator, times(1))
                .validate(jwtToken);
        verify(jwtClaimExtractor, times(1))
                .extractExpiration(jwtToken);

        //todo add tests for creating objects
    }
}