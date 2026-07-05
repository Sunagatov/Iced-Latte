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
import com.zufar.icedlatte.security.session.management.AuthSessionService;
import com.zufar.icedlatte.security.signin.auth.SecurityUserDetails;

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

    @Mock
    private AuthSessionService authSessionService;

    @Test
    @DisplayName("successfully authenticates user and attaches request details")
    void successfullyAuthenticatesUserAndAttachesRequestDetails() {
        JwtAuthenticationProvider jwtAuthenticationProvider = new JwtAuthenticationProvider(
                jwtBearerTokenResolver, jwtTokenClaims, userDetailsService, jwtTokenBlacklist, authSessionService);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setRemoteAddr("203.0.113.10");
        SecurityUserDetails userDetails = new SecurityUserDetails(
                java.util.UUID.randomUUID(),
                "test@example.com",
                "password",
                Collections.emptyList(),
                true,
                true,
                true,
                true);
        String jwtToken = "mockJwtToken";
        String userEmail = "test@example.com";
        var sessionId = java.util.UUID.randomUUID();

        when(jwtBearerTokenResolver.extract(httpRequest)).thenReturn(jwtToken);
        when(jwtTokenClaims.extractAccessTokenSessionId(jwtToken)).thenReturn(java.util.Optional.of(sessionId));
        when(jwtTokenClaims.extractAccessTokenEmail(jwtToken)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(userDetails);

        var authenticationToken = jwtAuthenticationProvider.get(httpRequest);

        assertThat(authenticationToken.getPrincipal()).isEqualTo(userDetails);
        assertThat(authenticationToken.getCredentials()).isNull();
        assertThat(authenticationToken.getDetails()).isInstanceOf(WebAuthenticationDetails.class);
        verify(jwtBearerTokenResolver).extract(httpRequest);
        verify(jwtTokenBlacklist).validateNotBlacklisted(jwtToken);
        verify(authSessionService).validateActiveSession(sessionId, userDetails.getId());
        verify(jwtTokenClaims).extractAccessTokenSessionId(jwtToken);
        verify(jwtTokenClaims).extractAccessTokenEmail(jwtToken);
        verify(userDetailsService).loadUserByUsername(userEmail);
        verifyNoMoreInteractions(
                jwtBearerTokenResolver, jwtTokenBlacklist, jwtTokenClaims, userDetailsService, authSessionService);
    }

    @Test
    @DisplayName("successfully authenticates raw authorization header")
    void successfullyAuthenticatesRawAuthorizationHeader() {
        JwtAuthenticationProvider jwtAuthenticationProvider = new JwtAuthenticationProvider(
                jwtBearerTokenResolver, jwtTokenClaims, userDetailsService, jwtTokenBlacklist, authSessionService);
        SecurityUserDetails userDetails = new SecurityUserDetails(
                java.util.UUID.randomUUID(),
                "test@example.com",
                "password",
                Collections.emptyList(),
                true,
                true,
                true,
                true);
        String authorizationHeader = "Bearer mockJwtToken";
        String jwtToken = "mockJwtToken";
        String userEmail = "test@example.com";
        var sessionId = java.util.UUID.randomUUID();

        when(jwtBearerTokenResolver.extract(authorizationHeader)).thenReturn(jwtToken);
        when(jwtTokenClaims.extractAccessTokenSessionId(jwtToken)).thenReturn(java.util.Optional.of(sessionId));
        when(jwtTokenClaims.extractAccessTokenEmail(jwtToken)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(userDetails);

        var authenticationToken = jwtAuthenticationProvider.get(authorizationHeader);

        assertThat(authenticationToken.getPrincipal()).isEqualTo(userDetails);
        assertThat(authenticationToken.getCredentials()).isNull();
        assertThat(authenticationToken.getDetails()).isNull();
        verify(jwtBearerTokenResolver).extract(authorizationHeader);
        verify(jwtTokenBlacklist).validateNotBlacklisted(jwtToken);
        verify(authSessionService).validateActiveSession(sessionId, userDetails.getId());
        verify(jwtTokenClaims).extractAccessTokenSessionId(jwtToken);
        verify(jwtTokenClaims).extractAccessTokenEmail(jwtToken);
        verify(userDetailsService).loadUserByUsername(userEmail);
        verifyNoMoreInteractions(
                jwtBearerTokenResolver, jwtTokenBlacklist, jwtTokenClaims, userDetailsService, authSessionService);
    }

    @Test
    @DisplayName("rejects access token when loaded user account is inactive")
    void rejectsAccessTokenWhenLoadedUserAccountIsInactive() {
        JwtAuthenticationProvider jwtAuthenticationProvider = new JwtAuthenticationProvider(
                jwtBearerTokenResolver, jwtTokenClaims, userDetailsService, jwtTokenBlacklist, authSessionService);
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

    @Test
    @DisplayName("rejects access token when session id is missing")
    void rejectsAccessTokenWhenSessionIdIsMissing() {
        JwtAuthenticationProvider jwtAuthenticationProvider = new JwtAuthenticationProvider(
                jwtBearerTokenResolver, jwtTokenClaims, userDetailsService, jwtTokenBlacklist, authSessionService);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        String jwtToken = "mockJwtToken";
        String userEmail = "test@example.com";
        SecurityUserDetails userDetails = new SecurityUserDetails(
                java.util.UUID.randomUUID(), userEmail, "password", Collections.emptyList(), true, true, true, true);

        when(jwtBearerTokenResolver.extract(httpRequest)).thenReturn(jwtToken);
        when(jwtTokenClaims.extractAccessTokenEmail(jwtToken)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(userDetails);
        when(jwtTokenClaims.extractAccessTokenSessionId(jwtToken)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> jwtAuthenticationProvider.get(httpRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("session");

        verifyNoInteractions(authSessionService);
    }

    @Test
    @DisplayName("rejects access token when loaded principal does not expose owner id")
    void rejectsAccessTokenWhenLoadedPrincipalDoesNotExposeOwnerId() {
        JwtAuthenticationProvider jwtAuthenticationProvider = new JwtAuthenticationProvider(
                jwtBearerTokenResolver, jwtTokenClaims, userDetailsService, jwtTokenBlacklist, authSessionService);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        String jwtToken = "mockJwtToken";
        String userEmail = "test@example.com";

        when(jwtBearerTokenResolver.extract(httpRequest)).thenReturn(jwtToken);
        when(jwtTokenClaims.extractAccessTokenEmail(jwtToken)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail))
                .thenReturn(new User(userEmail, "password", Collections.emptyList()));

        assertThatThrownBy(() -> jwtAuthenticationProvider.get(httpRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("session owner id");

        verifyNoInteractions(authSessionService);
    }

    @Test
    @DisplayName("rejects access token when session was revoked")
    void rejectsAccessTokenWhenSessionWasRevoked() {
        JwtAuthenticationProvider jwtAuthenticationProvider = new JwtAuthenticationProvider(
                jwtBearerTokenResolver, jwtTokenClaims, userDetailsService, jwtTokenBlacklist, authSessionService);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        SecurityUserDetails userDetails = new SecurityUserDetails(
                java.util.UUID.randomUUID(),
                "test@example.com",
                "password",
                Collections.emptyList(),
                true,
                true,
                true,
                true);
        String jwtToken = "mockJwtToken";
        String userEmail = "test@example.com";
        var sessionId = java.util.UUID.randomUUID();

        when(jwtBearerTokenResolver.extract(httpRequest)).thenReturn(jwtToken);
        when(jwtTokenClaims.extractAccessTokenEmail(jwtToken)).thenReturn(userEmail);
        when(jwtTokenClaims.extractAccessTokenSessionId(jwtToken)).thenReturn(java.util.Optional.of(sessionId));
        when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(userDetails);
        doThrow(new JwtTokenBlacklistedException("Session has been revoked"))
                .when(authSessionService)
                .validateActiveSession(sessionId, userDetails.getId());

        assertThatThrownBy(() -> jwtAuthenticationProvider.get(httpRequest))
                .isInstanceOf(JwtTokenBlacklistedException.class)
                .hasMessageContaining("revoked");
    }
}
