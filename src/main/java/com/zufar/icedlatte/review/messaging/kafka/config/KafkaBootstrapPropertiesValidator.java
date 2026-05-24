package com.zufar.icedlatte.review.messaging.kafka.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
public class KafkaBootstrapPropertiesValidator implements InitializingBean {

    private final KafkaIntegrationProperties integrationProperties;
    private final KafkaProperties kafkaProperties;

    @Override
    public void afterPropertiesSet() {
        if (!integrationProperties.enabled()) {
            return;
        }
        boolean missingBootstrapServers = kafkaProperties.getBootstrapServers().isEmpty()
                || kafkaProperties.getBootstrapServers().stream().allMatch(String::isBlank);
        if (missingBootstrapServers) {
            throw new IllegalStateException("spring.kafka.bootstrap-servers must not be blank when Kafka is enabled");
        }
    }
}
