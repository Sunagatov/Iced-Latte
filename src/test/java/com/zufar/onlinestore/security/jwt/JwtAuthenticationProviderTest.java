package com.zufar.onlinestore.security.jwt;

import jakarta.servlet.http.HttpServletRequest;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Mock
    private UserDetails userDetails;

    private String jwtToken = "TestJwtToken";
    private String userEmail = "TestEmail";

    @BeforeEach
    void setUp() {
        when(jwtTokenFromAuthHeaderExtractor.extract(httpRequest)).thenReturn(jwtToken);
        when(jwtClaimExtractor.extractEmail(jwtToken)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(userDetails);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("Given a valid JWT token in the request, When get method is called in JwtAuthenticationProvider, Then it should return a valid UsernamePasswordAuthenticationToken")
    void shouldReturnValidUsernamePasswordAuthenticationTokenWithValidJWT() {
        UsernamePasswordAuthenticationToken result = jwtAuthenticationProvider.get(httpRequest);

        assertNotNull(result);

        verify(jwtTokenFromAuthHeaderExtractor, times(1)).extract(httpRequest);
        verify(jwtBlacklistValidator, times(1)).validate(jwtToken);
        verify(jwtClaimExtractor, times(1)).extractExpiration(jwtToken);
        verify(userDetailsService, times(1)).loadUserByUsername(userEmail);

        UserDetails createdUserDetails = (UserDetails) ReflectionTestUtils.getField(result, "principal");
        assertEquals(userDetails, createdUserDetails);

        WebAuthenticationDetailsSource detailsSource = new WebAuthenticationDetailsSource();
        WebAuthenticationDetails details = detailsSource.buildDetails(httpRequest);
        assertEquals(details, ReflectionTestUtils.getField(result, "details"));
    }
}