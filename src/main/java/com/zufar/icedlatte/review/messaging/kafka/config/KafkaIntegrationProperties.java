package com.zufar.icedlatte.review.messaging.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

import static com.zufar.icedlatte.common.util.Preconditions.requireNotBlankOrThrow;
import static com.zufar.icedlatte.common.util.Preconditions.requirePositiveOrThrow;

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

        if (enabled) {
            requireNotBlankOrThrow(topics.reviewCreated(),
                    () -> new IllegalStateException("kafka.topics.review-created must not be blank when Kafka is enabled"));
            requireNotBlankOrThrow(consumerGroups.reviewAi(),
                    () -> new IllegalStateException("kafka.consumer-groups.review-ai must not be blank when Kafka is enabled"));
            if (!outbox.enabled()) {
                throw new IllegalStateException("kafka.outbox.enabled must be true when Kafka is enabled");
            }
            if (!inbox.enabled()) {
                throw new IllegalStateException("kafka.inbox.enabled must be true when Kafka is enabled");
            }
        }
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
            requirePositiveOrThrow(batchSize,
                    () -> new IllegalStateException("kafka.outbox.batch-size must be positive"));
            requirePositiveOrThrow(maxAttempts,
                    () -> new IllegalStateException("kafka.outbox.max-attempts must be positive"));
            pollInterval = pollInterval == null ? Duration.ofSeconds(5) : pollInterval;
            staleLockTimeout = staleLockTimeout == null ? Duration.ofMinutes(5) : staleLockTimeout;
            publishTimeout = publishTimeout == null ? Duration.ofSeconds(10) : publishTimeout;
            requirePositiveOrThrow(pollInterval,
                    () -> new IllegalStateException("kafka.outbox.poll-interval must be positive"));
            requirePositiveOrThrow(staleLockTimeout,
                    () -> new IllegalStateException("kafka.outbox.stale-lock-timeout must be positive"));
            requirePositiveOrThrow(publishTimeout,
                    () -> new IllegalStateException("kafka.outbox.publish-timeout must be positive"));
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
            requirePositiveOrThrow(batchSize,
                    () -> new IllegalStateException("kafka.inbox.batch-size must be positive"));
            requirePositiveOrThrow(maxAttempts,
                    () -> new IllegalStateException("kafka.inbox.max-attempts must be positive"));
            pollInterval = pollInterval == null ? Duration.ofSeconds(5) : pollInterval;
            staleLockTimeout = staleLockTimeout == null ? Duration.ofMinutes(5) : staleLockTimeout;
            requirePositiveOrThrow(pollInterval,
                    () -> new IllegalStateException("kafka.inbox.poll-interval must be positive"));
            requirePositiveOrThrow(staleLockTimeout,
                    () -> new IllegalStateException("kafka.inbox.stale-lock-timeout must be positive"));
            workerId = workerId == null || workerId.isBlank() ? "iced-latte-review-ai-inbox-worker" : workerId;
        }
    }
}
