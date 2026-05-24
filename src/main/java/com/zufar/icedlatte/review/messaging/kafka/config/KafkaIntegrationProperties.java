package com.zufar.icedlatte.review.messaging.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "kafka")
public record KafkaIntegrationProperties(
        boolean enabled,
        Topics topics,
        ConsumerGroups consumerGroups,
        Outbox outbox,
        Inbox inbox
) {

    public KafkaIntegrationProperties {
        topics = topics == null ? new Topics("iced-latte.review.created.v1") : topics;
        consumerGroups = consumerGroups == null ? new ConsumerGroups("iced-latte-review-ai") : consumerGroups;
        outbox = outbox == null ? Outbox.defaults() : outbox;
        inbox = inbox == null ? Inbox.defaults() : inbox;
    }

    public record Topics(String reviewCreated) {
    }

    public record ConsumerGroups(String reviewAi) {
    }

    public record Outbox(
            boolean enabled,
            boolean workerEnabled,
            int batchSize,
            int maxAttempts,
            Duration pollInterval,
            Duration staleLockTimeout,
            Duration publishTimeout,
            String workerId
    ) {

        public static Outbox defaults() {
            return new Outbox(true, true, 25, 10, Duration.ofSeconds(5), Duration.ofMinutes(5),
                    Duration.ofSeconds(10), "iced-latte-outbox-worker");
        }

        public Outbox {
            batchSize = batchSize <= 0 ? 25 : batchSize;
            maxAttempts = maxAttempts <= 0 ? 10 : maxAttempts;
            pollInterval = pollInterval == null ? Duration.ofSeconds(5) : pollInterval;
            staleLockTimeout = staleLockTimeout == null ? Duration.ofMinutes(5) : staleLockTimeout;
            publishTimeout = publishTimeout == null ? Duration.ofSeconds(10) : publishTimeout;
            workerId = workerId == null || workerId.isBlank() ? "iced-latte-outbox-worker" : workerId;
        }
    }

    public record Inbox(
            boolean enabled,
            boolean workerEnabled,
            int batchSize,
            int maxAttempts,
            Duration pollInterval,
            Duration staleLockTimeout,
            String workerId
    ) {

        public static Inbox defaults() {
            return new Inbox(true, true, 25, 10, Duration.ofSeconds(5), Duration.ofMinutes(5),
                    "iced-latte-review-ai-inbox-worker");
        }

        public Inbox {
            batchSize = batchSize <= 0 ? 25 : batchSize;
            maxAttempts = maxAttempts <= 0 ? 10 : maxAttempts;
            pollInterval = pollInterval == null ? Duration.ofSeconds(5) : pollInterval;
            staleLockTimeout = staleLockTimeout == null ? Duration.ofMinutes(5) : staleLockTimeout;
            workerId = workerId == null || workerId.isBlank() ? "iced-latte-review-ai-inbox-worker" : workerId;
        }
    }
}
