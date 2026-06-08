package com.zufar.icedlatte.security.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.zufar.icedlatte.security.jwt.config.JwtProperties;
import com.zufar.icedlatte.security.jwt.provider.JwtAuthenticationProvider;

@DisplayName("JwtStompAuthenticationChannelInterceptor unit tests")
class JwtStompAuthenticationChannelInterceptorTest {

    private final JwtAuthenticationProvider jwtAuthenticationProvider = mock(JwtAuthenticationProvider.class);
    private final MessageChannel channel = mock(MessageChannel.class);

    @Test
    @DisplayName("CONNECT frame authenticates from Authorization native header")
    void preSend_connectWithAuthorization_setsUser() {
        JwtStompAuthenticationChannelInterceptor interceptor =
                new JwtStompAuthenticationChannelInterceptor(properties(), jwtAuthenticationProvider);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("customer@example.com", null);
        when(jwtAuthenticationProvider.get("Bearer token")).thenReturn(authentication);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer token");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
        verify(jwtAuthenticationProvider).get("Bearer token");
    }

    @Test
    @DisplayName("CONNECT frame authenticates from access token handshake attribute")
    void preSend_connectWithCookieTokenAttribute_setsUser() {
        JwtStompAuthenticationChannelInterceptor interceptor =
                new JwtStompAuthenticationChannelInterceptor(properties(), jwtAuthenticationProvider);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("customer@example.com", null);
        when(jwtAuthenticationProvider.get("Bearer cookie-token")).thenReturn(authentication);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(Map.of(WebSocketAuthenticationAttributes.ACCESS_TOKEN_ATTRIBUTE, "cookie-token"));
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
        verify(jwtAuthenticationProvider).get("Bearer cookie-token");
    }

    @Test
    @DisplayName("CONNECT frame prefers Authorization native header over cookie token attribute")
    void preSend_connectWithAuthorizationAndCookieToken_prefersAuthorization() {
        JwtStompAuthenticationChannelInterceptor interceptor =
                new JwtStompAuthenticationChannelInterceptor(properties(), jwtAuthenticationProvider);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("customer@example.com", null);
        when(jwtAuthenticationProvider.get("Bearer header-token")).thenReturn(authentication);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer header-token");
        accessor.setSessionAttributes(Map.of(WebSocketAuthenticationAttributes.ACCESS_TOKEN_ATTRIBUTE, "cookie-token"));
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
        verify(jwtAuthenticationProvider).get("Bearer header-token");
        verify(jwtAuthenticationProvider, never()).get("Bearer cookie-token");
    }

    @Test
    @DisplayName("CONNECT frame without Authorization native header is rejected")
    void preSend_connectWithoutAuthorization_rejectsFrame() {
        JwtStompAuthenticationChannelInterceptor interceptor =
                new JwtStompAuthenticationChannelInterceptor(properties(), jwtAuthenticationProvider);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    private static JwtProperties properties() {
        return new JwtProperties(
                "Authorization",
                "01234567890123456789012345678901",
                "01234567890123456789012345678901",
                java.time.Duration.ofMinutes(15),
                java.time.Duration.ofDays(7),
                "iced-latte",
                "iced-latte");
    }
}
