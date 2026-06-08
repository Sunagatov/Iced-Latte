package com.zufar.icedlatte.supportchat.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.supportchat.config.SupportChatProperties.Bucket;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.OwnerMessageMode;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.RateLimits;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.Telegram;
import com.zufar.icedlatte.supportchat.config.SupportChatProperties.Turnstile;

@DisplayName("SupportChatProperties unit tests")
class SupportChatPropertiesTest {

    @Test
    @DisplayName("Telegram mode requires owner user id")
    void telegramMode_missingOwnerUserId_rejectsConfiguration() {
        assertThatThrownBy(() -> createProperties(new Telegram(
                        "bot-token",
                        "-1001234567890",
                        0L,
                        "webhook-secret",
                        true,
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(5))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("support-chat.telegram.owner-user-id is required when owner-message-mode=TELEGRAM");
    }

    @Test
    @DisplayName("Telegram mode requires webhook secret")
    void telegramMode_missingWebhookSecret_rejectsConfiguration() {
        assertThatThrownBy(() -> createProperties(new Telegram(
                        "bot-token", "-1001234567890", 555L, "", true, Duration.ofSeconds(3), Duration.ofSeconds(5))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("support-chat.telegram.webhook-secret is required when owner-message-mode=TELEGRAM");
    }

    @Test
    @DisplayName("Telegram mode accepts complete owner reply configuration")
    void telegramMode_completeConfiguration_isAccepted() {
        assertThatCode(() -> createProperties(new Telegram(
                        "bot-token",
                        "-1001234567890",
                        555L,
                        "webhook-secret",
                        true,
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(5))))
                .doesNotThrowAnyException();
    }

    private static void createProperties(Telegram telegram) {
        new SupportChatProperties(
                true,
                4000,
                90,
                OwnerMessageMode.TELEGRAM,
                telegram,
                new Turnstile(false),
                new RateLimits(
                        new Bucket(20, Duration.ofMinutes(1)),
                        new Bucket(100, Duration.ofHours(1)),
                        new Bucket(300, Duration.ofDays(1)),
                        new Bucket(10, Duration.ofSeconds(10))));
    }
}
