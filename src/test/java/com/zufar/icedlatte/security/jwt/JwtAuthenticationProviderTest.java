package com.zufar.icedlatte.security.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationProvider Tests")
class JwtAuthenticationProviderTest {

    @Mock
    private JwtBearerTokenResolver jwtBearerTokenResolver;

    @Mock
    private JwtTokenClaims jwtTokenClaims;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtTokenBlacklist jwtTokenBlacklist;

    @Test
    @DisplayName("successfully authenticates user and attaches request details")
    void successfullyAuthenticatesUserAndAttachesRequestDetails() {
        JwtAuthenticationProvider jwtAuthenticationProvider = new JwtAuthenticationProvider(
                jwtBearerTokenResolver,
                jwtTokenClaims,
                userDetailsService,
                jwtTokenBlacklist
        );
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setRemoteAddr("203.0.113.10");
        UserDetails userDetails = new User("test@example.com", "password", Collections.emptyList());
        String jwtToken = "mockJwtToken";
        String userEmail = "test@example.com";

        when(jwtBearerTokenResolver.extract(httpRequest)).thenReturn(jwtToken);
        when(jwtTokenClaims.extractAccessTokenEmail(jwtToken)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(userDetails);

        var authenticationToken = jwtAuthenticationProvider.get(httpRequest);

        assertThat(authenticationToken.getPrincipal()).isEqualTo(userDetails);
        assertThat(authenticationToken.getCredentials()).isNull();
        assertThat(authenticationToken.getDetails()).isInstanceOf(WebAuthenticationDetails.class);
        verify(jwtBearerTokenResolver).extract(httpRequest);
        verify(jwtTokenBlacklist).validateNotBlacklisted(jwtToken);
        verify(jwtTokenClaims).extractAccessTokenEmail(jwtToken);
        verify(userDetailsService).loadUserByUsername(userEmail);
        verifyNoMoreInteractions(jwtBearerTokenResolver, jwtTokenBlacklist, jwtTokenClaims, userDetailsService);
    }
}
