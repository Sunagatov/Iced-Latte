package com.zufar.icedlatte.security.websocket;

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
        String authorizationHeader = accessor.getFirstNativeHeader(jwtProperties.header());
        if (authorizationHeader == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required.");
        }
        accessor.setUser(jwtAuthenticationProvider.get(authorizationHeader));
    }
}
