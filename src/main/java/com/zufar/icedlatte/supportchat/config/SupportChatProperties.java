package com.zufar.icedlatte.supportchat.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "support-chat")
public record SupportChatProperties(
        boolean enabled,
        int messageMaxLength,
        int retentionDays,
        OwnerMessageMode ownerMessageMode,
        Turnstile turnstile,
        RateLimits rateLimits) {

    public SupportChatProperties {
        messageMaxLength = messageMaxLength == 0 ? 4000 : messageMaxLength;
        retentionDays = retentionDays == 0 ? 90 : retentionDays;
        ownerMessageMode = ownerMessageMode == null ? OwnerMessageMode.DISABLED : ownerMessageMode;
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
    }

    public enum OwnerMessageMode {
        DISABLED,
        FAKE
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
