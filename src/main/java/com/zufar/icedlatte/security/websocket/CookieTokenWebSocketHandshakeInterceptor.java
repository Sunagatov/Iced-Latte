package com.zufar.icedlatte.security.websocket;

import static com.zufar.icedlatte.security.websocket.WebSocketAuthenticationAttributes.ACCESS_TOKEN_ATTRIBUTE;
import static com.zufar.icedlatte.security.websocket.WebSocketAuthenticationAttributes.ACCESS_TOKEN_COOKIE_NAME;

import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class CookieTokenWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private static final String COOKIE_PAIR_SEPARATOR = ";";
    private static final String COOKIE_NAME_VALUE_SEPARATOR = "=";

    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            @NonNull Map<String, Object> attributes) {
        accessTokenCookie(request).ifPresent(token -> attributes.put(ACCESS_TOKEN_ATTRIBUTE, token));
        return true;
    }

    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler wsHandler,
            Exception exception) {}

    private static Optional<String> accessTokenCookie(ServerHttpRequest request) {
        return request.getHeaders().getOrEmpty(HttpHeaders.COOKIE).stream()
                .flatMap(header -> java.util.Arrays.stream(header.split(COOKIE_PAIR_SEPARATOR)))
                .map(String::trim)
                .filter(cookie -> cookie.startsWith(ACCESS_TOKEN_COOKIE_NAME + COOKIE_NAME_VALUE_SEPARATOR))
                .map(cookie -> cookie.substring(cookie.indexOf(COOKIE_NAME_VALUE_SEPARATOR) + 1))
                .filter(token -> !token.isBlank())
                .findFirst();
    }
}
