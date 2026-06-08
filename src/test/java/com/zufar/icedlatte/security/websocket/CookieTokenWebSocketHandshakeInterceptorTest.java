package com.zufar.icedlatte.security.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

@DisplayName("CookieTokenWebSocketHandshakeInterceptor unit tests")
class CookieTokenWebSocketHandshakeInterceptorTest {

    private final CookieTokenWebSocketHandshakeInterceptor interceptor = new CookieTokenWebSocketHandshakeInterceptor();
    private final ServerHttpResponse response = mock(ServerHttpResponse.class);
    private final WebSocketHandler webSocketHandler = mock(WebSocketHandler.class);

    @Test
    @DisplayName("Copies access token cookie to WebSocket session attributes")
    void beforeHandshake_withAccessTokenCookie_setsSessionAttribute() {
        var attributes = new HashMap<String, Object>();

        boolean result = interceptor.beforeHandshake(
                requestWithCookie("theme=light; token=access-token; refreshToken=refresh-token"),
                response,
                webSocketHandler,
                attributes);

        assertThat(result).isTrue();
        assertThat(attributes).containsEntry(WebSocketAuthenticationAttributes.ACCESS_TOKEN_ATTRIBUTE, "access-token");
    }

    @Test
    @DisplayName("Does not set access token attribute when cookie is absent")
    void beforeHandshake_withoutAccessTokenCookie_doesNotSetSessionAttribute() {
        var attributes = new HashMap<String, Object>();

        boolean result = interceptor.beforeHandshake(
                requestWithCookie("theme=light; refreshToken=refresh-token"), response, webSocketHandler, attributes);

        assertThat(result).isTrue();
        assertThat(attributes).doesNotContainKey(WebSocketAuthenticationAttributes.ACCESS_TOKEN_ATTRIBUTE);
    }

    private static ServerHttpRequest requestWithCookie(String cookie) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookie);

        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getHeaders()).thenReturn(headers);
        return request;
    }
}
