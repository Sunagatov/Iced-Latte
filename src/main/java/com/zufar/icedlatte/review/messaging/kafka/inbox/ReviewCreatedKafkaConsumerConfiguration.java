package com.zufar.icedlatte.review.messaging.kafka.inbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.zufar.icedlatte.review.messaging.kafka.config.KafkaIntegrationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

@Configuration
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
class ReviewCreatedKafkaConsumerConfiguration {

    @Bean
    ConcurrentMessageListenerContainer<String, String> reviewCreatedKafkaListenerContainer(
            ConsumerFactory<String, String> consumerFactory,
            KafkaIntegrationProperties properties,
            ReviewCreatedKafkaConsumer consumer
    ) {
        ContainerProperties containerProperties = new ContainerProperties(properties.topics().reviewCreated());
        containerProperties.setGroupId(properties.consumerGroups().reviewAi());
        containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL);
        containerProperties.setMessageListener((AcknowledgingMessageListener<String, String>) (record, acknowledgment) -> {
            try {
                consumer.consume(record, acknowledgment);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to deserialize review-created event", e);
            }
        });
        return new ConcurrentMessageListenerContainer<>(consumerFactory, containerProperties);
    }
}
