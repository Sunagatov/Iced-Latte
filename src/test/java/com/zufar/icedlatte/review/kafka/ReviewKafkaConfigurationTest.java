package com.zufar.icedlatte.review.kafka;

import com.zufar.icedlatte.review.messaging.kafka.config.KafkaIntegrationProperties;
import com.zufar.icedlatte.review.messaging.kafka.config.KafkaBootstrapPropertiesValidator;
import com.zufar.icedlatte.review.messaging.kafka.inbox.ReviewCreatedKafkaConsumer;
import com.zufar.icedlatte.review.messaging.kafka.outbox.ReviewCreatedKafkaPublisher;
import com.zufar.icedlatte.review.service.ai.ReviewCreatedApplicationEventListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Review Kafka configuration")
class ReviewKafkaConfigurationTest {

    @Test
    @DisplayName("local fallback listener is active only when Kafka is disabled")
    void localFallbackListenerIsActiveOnlyWhenKafkaIsDisabled() {
        ConditionalOnProperty condition =
                ReviewCreatedApplicationEventListener.class.getAnnotation(ConditionalOnProperty.class);

        assertThat(condition.prefix()).isEqualTo("kafka");
        assertThat(condition.name()).containsExactly("enabled");
        assertThat(condition.havingValue()).isEqualTo("false");
        assertThat(condition.matchIfMissing()).isTrue();
    }

    @Test
    @DisplayName("Kafka outbox and inbox components are active only when Kafka is enabled")
    void kafkaComponentsAreActiveOnlyWhenKafkaIsEnabled() throws Exception {
        assertKafkaEnabledCondition(ReviewCreatedKafkaPublisher.class);
        assertKafkaEnabledCondition(ReviewCreatedKafkaConsumer.class);
        assertKafkaEnabledCondition(Class.forName(
                "com.zufar.icedlatte.review.messaging.kafka.outbox.ReviewCreatedOutboxEventListener"));
        assertKafkaEnabledCondition(Class.forName(
                "com.zufar.icedlatte.review.messaging.kafka.inbox.ReviewCreatedInboxProcessor"));
    }

    @Test
    @DisplayName("allows Kafka-disabled configuration without outbox or inbox recording")
    void allowsKafkaDisabledConfigurationWithoutOutboxOrInboxRecording() {
        assertThatCode(() -> new KafkaIntegrationProperties(
                false,
                new KafkaIntegrationProperties.Topics(""),
                new KafkaIntegrationProperties.ConsumerGroups(""),
                new KafkaIntegrationProperties.Outbox(false, true, 25, 10, Duration.ofSeconds(5),
                        Duration.ofMinutes(5), Duration.ofSeconds(10), "test-outbox-worker"),
                new KafkaIntegrationProperties.Inbox(false, true, 25, 10, Duration.ofSeconds(5),
                        Duration.ofMinutes(5), "test-inbox-worker")
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejects Kafka-enabled configuration with blank topic")
    void rejectsKafkaEnabledConfigurationWithBlankTopic() {
        assertThatThrownBy(() -> createKafkaEnabledProperties(
                new KafkaIntegrationProperties.Topics(" "),
                new KafkaIntegrationProperties.ConsumerGroups("iced-latte-review-ai"),
                KafkaIntegrationProperties.Outbox.defaults(),
                KafkaIntegrationProperties.Inbox.defaults()
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("kafka.topics.review-created must not be blank when Kafka is enabled");
    }

    @Test
    @DisplayName("rejects Kafka-enabled configuration with disabled outbox recording")
    void rejectsKafkaEnabledConfigurationWithDisabledOutboxRecording() {
        assertThatThrownBy(() -> createKafkaEnabledProperties(
                new KafkaIntegrationProperties.Topics("iced-latte.review.created.v1"),
                new KafkaIntegrationProperties.ConsumerGroups("iced-latte-review-ai"),
                new KafkaIntegrationProperties.Outbox(false, true, 25, 10, Duration.ofSeconds(5),
                        Duration.ofMinutes(5), Duration.ofSeconds(10), "test-outbox-worker"),
                KafkaIntegrationProperties.Inbox.defaults()
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("kafka.outbox.enabled must be true when Kafka is enabled");
    }

    @Test
    @DisplayName("rejects Kafka-enabled configuration with blank consumer group")
    void rejectsKafkaEnabledConfigurationWithBlankConsumerGroup() {
        assertThatThrownBy(() -> createKafkaEnabledProperties(
                new KafkaIntegrationProperties.Topics("iced-latte.review.created.v1"),
                new KafkaIntegrationProperties.ConsumerGroups(" "),
                KafkaIntegrationProperties.Outbox.defaults(),
                KafkaIntegrationProperties.Inbox.defaults()
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("kafka.consumer-groups.review-ai must not be blank when Kafka is enabled");
    }

    @Test
    @DisplayName("rejects Kafka-enabled configuration with disabled inbox recording")
    void rejectsKafkaEnabledConfigurationWithDisabledInboxRecording() {
        assertThatThrownBy(() -> createKafkaEnabledProperties(
                new KafkaIntegrationProperties.Topics("iced-latte.review.created.v1"),
                new KafkaIntegrationProperties.ConsumerGroups("iced-latte-review-ai"),
                KafkaIntegrationProperties.Outbox.defaults(),
                new KafkaIntegrationProperties.Inbox(false, true, 25, 10, Duration.ofSeconds(5),
                        Duration.ofMinutes(5), "test-inbox-worker")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("kafka.inbox.enabled must be true when Kafka is enabled");
    }

    @Test
    @DisplayName("rejects invalid outbox worker settings")
    void rejectsInvalidOutboxWorkerSettings() {
        assertThatThrownBy(() -> new KafkaIntegrationProperties.Outbox(true, true, 0, 10,
                Duration.ofSeconds(5), Duration.ofMinutes(5), Duration.ofSeconds(10), "test-outbox-worker"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("kafka.outbox.batch-size must be positive");
    }

    @Test
    @DisplayName("rejects invalid inbox worker settings")
    void rejectsInvalidInboxWorkerSettings() {
        assertThatThrownBy(() -> new KafkaIntegrationProperties.Inbox(true, true, 25, 10,
                Duration.ZERO, Duration.ofMinutes(5), "test-inbox-worker"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("kafka.inbox.poll-interval must be positive");
    }

    @Test
    @DisplayName("rejects Kafka-enabled configuration with blank bootstrap servers")
    void rejectsKafkaEnabledConfigurationWithBlankBootstrapServers() {
        KafkaProperties kafkaProperties = new KafkaProperties();
        kafkaProperties.setBootstrapServers(List.of(" "));

        var validator = new KafkaBootstrapPropertiesValidator(kafkaEnabledProperties(), kafkaProperties);

        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("spring.kafka.bootstrap-servers must not be blank when Kafka is enabled");
    }

    private void assertKafkaEnabledCondition(Class<?> componentClass) {
        ConditionalOnProperty condition = componentClass.getAnnotation(ConditionalOnProperty.class);

        assertThat(condition.prefix()).isEqualTo("kafka");
        assertThat(condition.name()).containsExactly("enabled");
        assertThat(condition.havingValue()).isEqualTo("true");
        assertThat(condition.matchIfMissing()).isFalse();
    }

    private void createKafkaEnabledProperties(KafkaIntegrationProperties.Topics topics,
                                              KafkaIntegrationProperties.ConsumerGroups consumerGroups,
                                              KafkaIntegrationProperties.Outbox outbox,
                                              KafkaIntegrationProperties.Inbox inbox) {
        new KafkaIntegrationProperties(true, topics, consumerGroups, outbox, inbox);
    }

    private KafkaIntegrationProperties kafkaEnabledProperties() {
        return new KafkaIntegrationProperties(
                true,
                new KafkaIntegrationProperties.Topics("iced-latte.review.created.v1"),
                new KafkaIntegrationProperties.ConsumerGroups("iced-latte-review-ai"),
                KafkaIntegrationProperties.Outbox.defaults(),
                KafkaIntegrationProperties.Inbox.defaults()
        );
    }
}
