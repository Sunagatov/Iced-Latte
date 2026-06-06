package com.zufar.icedlatte.security.jwt.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.Collections;

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

import com.zufar.icedlatte.security.jwt.blacklist.JwtTokenBlacklist;
import com.zufar.icedlatte.security.jwt.exception.JwtTokenBlacklistedException;
import com.zufar.icedlatte.security.jwt.resolver.JwtBearerTokenResolver;
import com.zufar.icedlatte.security.jwt.resolver.JwtTokenClaims;

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
                jwtBearerTokenResolver, jwtTokenClaims, userDetailsService, jwtTokenBlacklist);
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

    @Test
    @DisplayName("rejects access token when loaded user account is inactive")
    void rejectsAccessTokenWhenLoadedUserAccountIsInactive() {
        JwtAuthenticationProvider jwtAuthenticationProvider = new JwtAuthenticationProvider(
                jwtBearerTokenResolver, jwtTokenClaims, userDetailsService, jwtTokenBlacklist);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        UserDetails userDetails = User.withUsername("locked@example.com")
                .password("password")
                .authorities(Collections.emptyList())
                .accountLocked(true)
                .build();
        String jwtToken = "mockJwtToken";
        String userEmail = "locked@example.com";

        when(jwtBearerTokenResolver.extract(httpRequest)).thenReturn(jwtToken);
        when(jwtTokenClaims.extractAccessTokenEmail(jwtToken)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(userDetails);

        assertThatThrownBy(() -> jwtAuthenticationProvider.get(httpRequest))
                .isInstanceOf(JwtTokenBlacklistedException.class);
    }
}
