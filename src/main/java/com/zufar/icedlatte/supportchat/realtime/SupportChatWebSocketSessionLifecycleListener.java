package com.zufar.icedlatte.supportchat.realtime;

import org.jspecify.annotations.NonNull;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SupportChatWebSocketSessionLifecycleListener {

    private final SupportChatWebSocketSessionRegistry sessionRegistry;

    @EventListener
    public void onDisconnect(@NonNull SessionDisconnectEvent event) {
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
        if (sessionId != null && !sessionId.isBlank()) {
            sessionRegistry.unregister(sessionId);
        }
    }
}
