package com.zufar.icedlatte.review.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.zufar.icedlatte.review.messaging.kafka.config.KafkaIntegrationProperties;
import com.zufar.icedlatte.review.messaging.kafka.event.ReviewCreatedKafkaEvent;
import com.zufar.icedlatte.review.messaging.kafka.inbox.InboxEventRepository;
import com.zufar.icedlatte.review.messaging.kafka.inbox.ReviewCreatedKafkaConsumer;
import com.zufar.icedlatte.review.messaging.kafka.outbox.OutboxEventRepository;
import com.zufar.icedlatte.review.messaging.kafka.outbox.ReviewCreatedKafkaPublisher;

@Testcontainers
@DisplayName("Review Kafka container flow")
class ReviewKafkaContainerFlowTest {

    private static final String TOPIC = "iced-latte.review.created.v1";

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @BeforeAll
    static void createTopic() throws Exception {
        Map<String, Object> bootstrapServersConfig =
                Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        try (AdminClient adminClient = AdminClient.create(bootstrapServersConfig)) {
            adminClient
                    .createTopics(List.of(new NewTopic(TOPIC, 1, (short) 1)))
                    .all()
                    .get();
        }
    }

    @Test
    @DisplayName("publishes outbox JSON to Kafka and records consumed message in inbox")
    void publishesOutboxJsonToKafkaAndRecordsConsumedMessageInInbox() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ObjectMapper objectMapper =
                new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ReviewCreatedKafkaEvent event = new ReviewCreatedKafkaEvent(
                eventId,
                "review.created",
                1,
                "iced-latte",
                Instant.parse("2026-05-24T12:00:00Z"),
                null,
                null,
                new ReviewCreatedKafkaEvent.Payload(reviewId, productId));
        String payload = objectMapper.writeValueAsString(event);
        var row = new OutboxEventRepository.OutboxEventRow(
                UUID.randomUUID(), eventId, TOPIC, productId.toString(), payload, "{}", 0, 10);
        OutboxEventRepository outboxEventRepository = mock(OutboxEventRepository.class);
        when(outboxEventRepository.claimPublishableEvents(25, "test-outbox-worker"))
                .thenReturn(List.of(row));
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplate();
        KafkaIntegrationProperties properties = kafkaProperties();

        try (Consumer<String, String> rawConsumer = rawConsumer()) {
            rawConsumer.subscribe(List.of(TOPIC));

            new ReviewCreatedKafkaPublisher(kafkaTemplate, objectMapper, properties, outboxEventRepository)
                    .publishPendingOutboxEvents();

            ConsumerRecord<String, String> kafkaRecord =
                    KafkaTestUtils.getSingleRecord(rawConsumer, TOPIC, Duration.ofSeconds(10));
            assertThat(kafkaRecord.key()).isEqualTo(productId.toString());
            assertThat(kafkaRecord.value()).isEqualTo(payload);

            InboxEventRepository inboxEventRepository = mock(InboxEventRepository.class);
            Acknowledgment acknowledgment = mock(Acknowledgment.class);
            when(inboxEventRepository.insertReceivedEvent(
                            any(ReviewCreatedKafkaEvent.class),
                            eq(TOPIC),
                            eq(productId.toString()),
                            eq(kafkaRecord.partition()),
                            eq(kafkaRecord.offset()),
                            eq("iced-latte-review-ai"),
                            eq(payload),
                            eq("{}"),
                            eq(10)))
                    .thenReturn(true);

            new ReviewCreatedKafkaConsumer(objectMapper, properties, inboxEventRepository)
                    .consume(kafkaRecord, acknowledgment);

            verify(acknowledgment).acknowledge();
            verify(outboxEventRepository)
                    .markPublished(row.id(), "test-outbox-worker", kafkaRecord.partition(), kafkaRecord.offset());
        } finally {
            kafkaTemplate.destroy();
        }
    }

    private KafkaTemplate<String, String> kafkaTemplate() {
        var producerProperties = new HashMap<String, Object>();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        var producerFactory = new DefaultKafkaProducerFactory<String, String>(producerProperties);
        return new KafkaTemplate<>(producerFactory);
    }

    private Consumer<String, String> rawConsumer() {
        var consumerProperties = new HashMap<String, Object>();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "review-container-flow");
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        return new DefaultKafkaConsumerFactory<>(consumerProperties, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
    }

    private KafkaIntegrationProperties kafkaProperties() {
        return new KafkaIntegrationProperties(
                true,
                new KafkaIntegrationProperties.Topics(TOPIC),
                new KafkaIntegrationProperties.ConsumerGroups("iced-latte-review-ai"),
                new KafkaIntegrationProperties.Outbox(
                        true,
                        true,
                        25,
                        10,
                        Duration.ofSeconds(5),
                        Duration.ofMinutes(5),
                        Duration.ofSeconds(10),
                        "test-outbox-worker"),
                new KafkaIntegrationProperties.Inbox(
                        true, true, 25, 10, Duration.ofSeconds(5), Duration.ofMinutes(5), "test-inbox-worker"));
    }
}
