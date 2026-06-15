package com.zufar.icedlatte.supportchat.service;

import com.zufar.icedlatte.supportchat.config.SupportChatProperties;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.Bucket;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.OwnerMessageMode;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.RateLimits;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.Telegram;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.Turnstile;
import com.zufar.icedlatte.supportchat.entity.SupportMessageEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SupportChatAbuseGuard unit tests")
class SupportChatAbuseGuardTest {

    @Test
    @DisplayName("Cooldown still requires Turnstile when first-message challenge is disabled")
    void requiresTurnstile_cooldownActive_ignoresFirstMessageFlag() {
        UUID conversationId = UUID.randomUUID();
        Clock clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);
        SupportChatAbuseGuard guard = new SupportChatAbuseGuard(properties(), clock);

        guard.requireTurnstileForNextMessage(conversationId);

        assertThat(guard.requiresTurnstile(recentMessage(clock), conversationId))
                .isTrue();
    }

    @Test
    @DisplayName("Long inactivity still requires Turnstile when first-message challenge is disabled")
    void requiresTurnstile_longInactivity_ignoresFirstMessageFlag() {
        UUID conversationId = UUID.randomUUID();
        Clock clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);
        SupportChatAbuseGuard guard = new SupportChatAbuseGuard(properties(), clock);

        assertThat(guard.requiresTurnstile(staleMessage(clock), conversationId)).isTrue();
    }

    private static SupportMessageEntity recentMessage(Clock clock) {
        SupportMessageEntity message = new SupportMessageEntity();
        message.setCreatedAt(OffsetDateTime.now(clock).minusMinutes(1));
        return message;
    }

    private static SupportMessageEntity staleMessage(Clock clock) {
        SupportMessageEntity message = new SupportMessageEntity();
        message.setCreatedAt(OffsetDateTime.now(clock).minusDays(2));
        return message;
    }

    private static SupportChatProperties properties() {
        return new SupportChatProperties(
                true,
                4000,
                90,
                OwnerMessageMode.FAKE,
                new Telegram("", "", 0L, "", true, Duration.ofSeconds(3), Duration.ofSeconds(5)),
                new Turnstile(false, Duration.ofHours(24), Duration.ofMinutes(5)),
                new RateLimits(
                        new Bucket(20, Duration.ofMinutes(1)),
                        new Bucket(100, Duration.ofHours(1)),
                        new Bucket(300, Duration.ofDays(1)),
                        new Bucket(10, Duration.ofSeconds(10)),
                        new Bucket(60, Duration.ofMinutes(1))),
                Set.of());
    }
}
