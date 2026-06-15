package com.zufar.icedlatte.supportchat.config;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "support-chat")
public record SupportChatProperties(
        boolean enabled,
        int messageMaxLength,
        int retentionDays,
        OwnerMessageMode ownerMessageMode,
        Telegram telegram,
        Turnstile turnstile,
        RateLimits rateLimits,
        Set<String> allowedEmails) {

    public SupportChatProperties {
        messageMaxLength = messageMaxLength == 0 ? 4000 : messageMaxLength;
        retentionDays = retentionDays == 0 ? 90 : retentionDays;
        ownerMessageMode = ownerMessageMode == null ? OwnerMessageMode.DISABLED : ownerMessageMode;
        telegram = telegram == null ? Telegram.disabled() : telegram;
        turnstile = turnstile == null ? Turnstile.defaults() : turnstile;
        allowedEmails = normalizeAllowedEmails(allowedEmails);
        rateLimits = rateLimits == null
                ? new RateLimits(
                        new Bucket(20, Duration.ofMinutes(1)),
                        new Bucket(100, Duration.ofHours(1)),
                        new Bucket(300, Duration.ofDays(1)),
                        new Bucket(10, Duration.ofSeconds(10)),
                        new Bucket(60, Duration.ofMinutes(1)))
                : rateLimits;
        if (messageMaxLength < 1 || messageMaxLength > 4000) {
            throw new IllegalStateException("support-chat.message-max-length must be between 1 and 4000");
        }
        if (retentionDays < 1) {
            throw new IllegalStateException("support-chat.retention-days must be positive");
        }
        if (enabled && ownerMessageMode == OwnerMessageMode.DISABLED) {
            throw new IllegalStateException(
                    "support-chat.owner-message-mode must not be DISABLED when support-chat.enabled=true");
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

    private static Set<String> normalizeAllowedEmails(Set<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return Set.of();
        }
        return emails.stream()
                .map(email -> email == null ? "" : email.trim().toLowerCase(Locale.ROOT))
                .filter(email -> !email.isBlank())
                .collect(Collectors.toUnmodifiableSet());
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

    public record Turnstile(
            boolean firstMessageEnabled, Duration longInactivityDuration, Duration abuseCooldownDuration) {

        public Turnstile {
            longInactivityDuration = longInactivityDuration == null ? Duration.ofHours(24) : longInactivityDuration;
            abuseCooldownDuration = abuseCooldownDuration == null ? Duration.ofMinutes(5) : abuseCooldownDuration;
            if (longInactivityDuration.isZero() || longInactivityDuration.isNegative()) {
                throw new IllegalStateException("support-chat.turnstile.long-inactivity-duration must be positive");
            }
            if (abuseCooldownDuration.isZero() || abuseCooldownDuration.isNegative()) {
                throw new IllegalStateException("support-chat.turnstile.abuse-cooldown-duration must be positive");
            }
        }

        private static Turnstile defaults() {
            return new Turnstile(false, Duration.ofHours(24), Duration.ofMinutes(5));
        }
    }

    public record RateLimits(
            Bucket perMinute, Bucket perHour, Bucket perDay, Bucket perConversationBurst, Bucket perIp) {

        public RateLimits {
            perMinute = perMinute == null ? new Bucket(20, Duration.ofMinutes(1)) : perMinute;
            perHour = perHour == null ? new Bucket(100, Duration.ofHours(1)) : perHour;
            perDay = perDay == null ? new Bucket(300, Duration.ofDays(1)) : perDay;
            perConversationBurst =
                    perConversationBurst == null ? new Bucket(10, Duration.ofSeconds(10)) : perConversationBurst;
            perIp = perIp == null ? new Bucket(60, Duration.ofMinutes(1)) : perIp;
        }
    }

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
