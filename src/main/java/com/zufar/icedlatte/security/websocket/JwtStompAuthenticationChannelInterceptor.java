package com.zufar.icedlatte.security.websocket;

import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

import com.zufar.icedlatte.security.jwt.config.JwtProperties;
import com.zufar.icedlatte.security.jwt.provider.JwtAuthenticationProvider;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtStompAuthenticationChannelInterceptor implements ChannelInterceptor {

    private final JwtProperties jwtProperties;
    private final JwtAuthenticationProvider jwtAuthenticationProvider;

    public JwtStompAuthenticationChannelInterceptor(
            JwtProperties jwtProperties, JwtAuthenticationProvider jwtAuthenticationProvider) {
        this.jwtProperties = jwtProperties;
        this.jwtAuthenticationProvider = jwtAuthenticationProvider;
    }

    @Override
    public @Nullable Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        Optional<String> authorizationHeader = authorizationHeader(accessor);
        if (authorizationHeader.isEmpty()) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required.");
        }
        accessor.setUser(jwtAuthenticationProvider.get(authorizationHeader.get()));
    }

    private Optional<String> authorizationHeader(StompHeaderAccessor accessor) {
        String nativeAuthorizationHeader = accessor.getFirstNativeHeader(jwtProperties.header());
        if (nativeAuthorizationHeader != null) {
            return Optional.of(nativeAuthorizationHeader);
        }

        return accessTokenAttribute(accessor).map(token -> "Bearer " + token);
    }

    private static Optional<String> accessTokenAttribute(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return Optional.empty();
        }

        Object token = sessionAttributes.get(WebSocketAuthenticationAttributes.ACCESS_TOKEN_ATTRIBUTE);
        return token instanceof String value && !value.isBlank() ? Optional.of(value) : Optional.empty();
    }
}
