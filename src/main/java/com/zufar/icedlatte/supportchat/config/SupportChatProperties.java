package com.zufar.icedlatte.supportchat.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "support-chat")
public record SupportChatProperties(
        boolean enabled,
        int messageMaxLength,
        int retentionDays,
        OwnerMessageMode ownerMessageMode,
        Telegram telegram,
        Turnstile turnstile,
        RateLimits rateLimits) {

    public SupportChatProperties {
        messageMaxLength = messageMaxLength == 0 ? 4000 : messageMaxLength;
        retentionDays = retentionDays == 0 ? 90 : retentionDays;
        ownerMessageMode = ownerMessageMode == null ? OwnerMessageMode.DISABLED : ownerMessageMode;
        telegram = telegram == null ? Telegram.disabled() : telegram;
        turnstile = turnstile == null ? new Turnstile(false) : turnstile;
        rateLimits = rateLimits == null
                ? new RateLimits(
                        new Bucket(20, Duration.ofMinutes(1)),
                        new Bucket(100, Duration.ofHours(1)),
                        new Bucket(300, Duration.ofDays(1)),
                        new Bucket(10, Duration.ofSeconds(10)))
                : rateLimits;
        if (messageMaxLength < 1 || messageMaxLength > 4000) {
            throw new IllegalStateException("support-chat.message-max-length must be between 1 and 4000");
        }
        if (retentionDays < 1) {
            throw new IllegalStateException("support-chat.retention-days must be positive");
        }
        if (ownerMessageMode == OwnerMessageMode.TELEGRAM) {
            if (telegram.botToken().isBlank()) {
                throw new IllegalStateException(
                        "support-chat.telegram.bot-token is required when owner-message-mode=TELEGRAM");
            }
            if (telegram.chatId().isBlank()) {
                throw new IllegalStateException(
                        "support-chat.telegram.chat-id is required when owner-message-mode=TELEGRAM");
            }
            if (telegram.ownerUserId() < 1) {
                throw new IllegalStateException(
                        "support-chat.telegram.owner-user-id is required when owner-message-mode=TELEGRAM");
            }
            if (telegram.webhookSecret().isBlank()) {
                throw new IllegalStateException(
                        "support-chat.telegram.webhook-secret is required when owner-message-mode=TELEGRAM");
            }
        }
    }

    public enum OwnerMessageMode {
        DISABLED,
        FAKE,
        TELEGRAM
    }

    public record Telegram(
            String botToken,
            String chatId,
            Long ownerUserId,
            String webhookSecret,
            boolean forumTopicsEnabled,
            Duration connectTimeout,
            Duration readTimeout) {

        public Telegram {
            botToken = botToken == null ? "" : botToken;
            chatId = chatId == null ? "" : chatId;
            ownerUserId = ownerUserId == null ? 0L : ownerUserId;
            webhookSecret = webhookSecret == null ? "" : webhookSecret;
            connectTimeout = connectTimeout == null ? Duration.ofSeconds(3) : connectTimeout;
            readTimeout = readTimeout == null ? Duration.ofSeconds(5) : readTimeout;
            if (connectTimeout.isZero() || connectTimeout.isNegative()) {
                throw new IllegalStateException("support-chat.telegram.connect-timeout must be positive");
            }
            if (readTimeout.isZero() || readTimeout.isNegative()) {
                throw new IllegalStateException("support-chat.telegram.read-timeout must be positive");
            }
        }

        private static Telegram disabled() {
            return new Telegram("", "", 0L, "", true, Duration.ofSeconds(3), Duration.ofSeconds(5));
        }
    }

    public record Turnstile(boolean firstMessageEnabled) {}

    public record RateLimits(Bucket perMinute, Bucket perHour, Bucket perDay, Bucket perConversationBurst) {}

    public record Bucket(int maxRequests, Duration windowDuration) {

        public Bucket {
            if (maxRequests < 1) {
                throw new IllegalStateException("support-chat rate limit maxRequests must be positive");
            }
            if (windowDuration == null || windowDuration.isZero() || windowDuration.isNegative()) {
                throw new IllegalStateException("support-chat rate limit windowDuration must be positive");
            }
        }
    }
}
