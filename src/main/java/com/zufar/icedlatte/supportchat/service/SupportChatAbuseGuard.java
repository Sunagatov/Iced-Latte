package com.zufar.icedlatte.supportchat.service;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.entity.SupportMessageEntity;

@Component
public class SupportChatAbuseGuard {

    private final SupportChatProperties properties;
    private final Clock clock;
    private final ConcurrentMap<UUID, OffsetDateTime> challengeRequiredUntil = new ConcurrentHashMap<>();

    @Autowired
    SupportChatAbuseGuard(SupportChatProperties properties) {
        this(properties, Clock.systemUTC());
    }

    SupportChatAbuseGuard(SupportChatProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    boolean requiresTurnstile(SupportMessageEntity previousCustomerMessage, UUID conversationId) {
        if (isChallengeCooldownActive(conversationId)) {
            return true;
        }
        if (previousCustomerMessage == null) {
            return properties.turnstile().firstMessageEnabled();
        }

        OffsetDateTime cutoff = now().minus(properties.turnstile().longInactivityDuration());
        return previousCustomerMessage.getCreatedAt().isBefore(cutoff);
    }

    void requireTurnstileForNextMessage(UUID conversationId) {
        Duration abuseCooldownDuration = properties.turnstile().abuseCooldownDuration();
        OffsetDateTime dateTime = now().plus(abuseCooldownDuration);
        challengeRequiredUntil.put(conversationId, dateTime);
    }

    void clearTurnstileRequirement(UUID conversationId) {
        challengeRequiredUntil.remove(conversationId);
    }

    private boolean isChallengeCooldownActive(UUID conversationId) {
        OffsetDateTime expiresAt = challengeRequiredUntil.get(conversationId);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isAfter(now())) {
            return true;
        }
        challengeRequiredUntil.remove(conversationId, expiresAt);
        return false;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
