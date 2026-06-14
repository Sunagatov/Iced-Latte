package com.zufar.icedlatte.security.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.security.config.cors.CorsProperties;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketMessageBrokerConfiguration implements WebSocketMessageBrokerConfigurer {

    private static final String[] BROKER_DESTINATION_PREFIXES = {"/topic", "/queue"};

    private final CorsProperties corsProperties;
    private final ObjectProvider<ChannelInterceptor> channelInterceptors;
    private final CookieTokenWebSocketHandshakeInterceptor cookieTokenWebSocketHandshakeInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(ApiPaths.WEBSOCKET)
                .addInterceptors(cookieTokenWebSocketHandshakeInterceptor)
                .setAllowedOriginPatterns(corsProperties.allowedOrigins().toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(BROKER_DESTINATION_PREFIXES);
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(channelInterceptors.orderedStream().toArray(ChannelInterceptor[]::new));
    }
}
