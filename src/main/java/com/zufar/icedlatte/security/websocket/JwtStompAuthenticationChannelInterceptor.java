package com.zufar.icedlatte.security.websocket;

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

import lombok.RequiredArgsConstructor;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class JwtStompAuthenticationChannelInterceptor implements ChannelInterceptor {

    private final JwtProperties jwtProperties;
    private final SupportChatWebSocketAuthenticationService supportChatWebSocketAuthenticationService;

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
        accessor.setUser(supportChatWebSocketAuthenticationService.authenticate(authorizationHeader.get()));
    }

    private Optional<String> authorizationHeader(StompHeaderAccessor accessor) {
        String nativeAuthorizationHeader = accessor.getFirstNativeHeader(jwtProperties.header());
        return Optional.ofNullable(nativeAuthorizationHeader).filter(header -> !header.isBlank());
    }
}
