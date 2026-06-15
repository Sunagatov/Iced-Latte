package com.zufar.icedlatte.security.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.zufar.icedlatte.security.jwt.blacklist.JwtTokenBlacklist;
import com.zufar.icedlatte.security.jwt.exception.JwtTokenException;
import com.zufar.icedlatte.security.jwt.resolver.JwtBearerTokenResolver;
import com.zufar.icedlatte.security.jwt.resolver.JwtTokenClaims;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupportChatWebSocketAuthenticationService unit tests")
class SupportChatWebSocketAuthenticationServiceTest {

    @Mock
    private JwtBearerTokenResolver jwtBearerTokenResolver;

    @Mock
    private JwtTokenClaims jwtTokenClaims;

    @Mock
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Mock
    private JwtTokenBlacklist jwtTokenBlacklist;

    @Test
    @DisplayName("authenticates support chat websocket ticket from authorization header")
    void authenticatesSupportChatWebSocketTicketFromAuthorizationHeader() {
        SupportChatWebSocketAuthenticationService service = new SupportChatWebSocketAuthenticationService(
                jwtBearerTokenResolver, jwtTokenClaims, userDetailsService, jwtTokenBlacklist);
        UserDetails userDetails = new User("test@example.com", "password", Collections.emptyList());
        String authorizationHeader = "Bearer ws-ticket";
        String jwtToken = "ws-ticket";
        String userEmail = "test@example.com";

        when(jwtBearerTokenResolver.extract(authorizationHeader)).thenReturn(jwtToken);
        when(jwtTokenClaims.extractSupportChatWebSocketTicketEmail(jwtToken)).thenReturn(userEmail);
        when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(userDetails);

        var authenticationToken = service.authenticate(authorizationHeader);

        assertThat(authenticationToken.getPrincipal()).isEqualTo(userDetails);
        verify(jwtBearerTokenResolver).extract(authorizationHeader);
        verify(jwtTokenBlacklist).validateNotBlacklisted(jwtToken);
        verify(jwtTokenClaims).extractSupportChatWebSocketTicketEmail(jwtToken);
        verify(userDetailsService).loadUserByUsername(userEmail);
        verifyNoMoreInteractions(jwtBearerTokenResolver, jwtTokenBlacklist, jwtTokenClaims, userDetailsService);
    }

    @Test
    @DisplayName("rejects plain access token on support chat websocket auth path")
    void rejectsPlainAccessTokenOnSupportChatWebSocketAuthPath() {
        SupportChatWebSocketAuthenticationService service = new SupportChatWebSocketAuthenticationService(
                jwtBearerTokenResolver, jwtTokenClaims, userDetailsService, jwtTokenBlacklist);
        String authorizationHeader = "Bearer access-token";
        String jwtToken = "access-token";

        when(jwtBearerTokenResolver.extract(authorizationHeader)).thenReturn(jwtToken);
        when(jwtTokenClaims.extractSupportChatWebSocketTicketEmail(jwtToken))
                .thenThrow(new JwtTokenException("Invalid support chat WebSocket ticket"));

        assertThatThrownBy(() -> service.authenticate(authorizationHeader)).isInstanceOf(JwtTokenException.class);
        verify(jwtBearerTokenResolver).extract(authorizationHeader);
        verify(jwtTokenBlacklist).validateNotBlacklisted(jwtToken);
        verify(jwtTokenClaims).extractSupportChatWebSocketTicketEmail(jwtToken);
        verifyNoMoreInteractions(jwtBearerTokenResolver, jwtTokenBlacklist, jwtTokenClaims, userDetailsService);
    }
}
