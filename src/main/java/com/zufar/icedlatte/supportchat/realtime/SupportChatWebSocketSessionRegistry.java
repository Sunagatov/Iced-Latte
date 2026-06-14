package com.zufar.icedlatte.supportchat.realtime;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SupportChatWebSocketSessionRegistry {

    private final int maxSessionsPerUser;
    private final ConcurrentMap<String, UUID> sessionOwners = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Set<String>> userSessions = new ConcurrentHashMap<>();

    SupportChatWebSocketSessionRegistry(
            @Value("${support-chat.websocket.max-active-sessions-per-user:5}") int maxSessionsPerUser) {
        if (maxSessionsPerUser < 1) {
            throw new IllegalStateException("support-chat.websocket.max-active-sessions-per-user must be positive");
        }
        this.maxSessionsPerUser = maxSessionsPerUser;
    }

    synchronized boolean register(UUID userId, String sessionId) {
        UUID existingOwner = sessionOwners.get(sessionId);
        if (userId.equals(existingOwner)) {
            return true;
        }
        if (existingOwner != null) {
            return false;
        }

        Set<String> sessions = userSessions.computeIfAbsent(userId, _ -> ConcurrentHashMap.newKeySet());
        if (sessions.size() >= maxSessionsPerUser) {
            return false;
        }

        sessionOwners.put(sessionId, userId);
        sessions.add(sessionId);
        return true;
    }

    synchronized void unregister(String sessionId) {
        UUID userId = sessionOwners.remove(sessionId);
        if (userId == null) {
            return;
        }

        Set<String> sessions = userSessions.get(userId);
        if (sessions == null) {
            return;
        }

        sessions.remove(sessionId);
        if (sessions.isEmpty()) {
            userSessions.remove(userId, sessions);
        }
    }
}
