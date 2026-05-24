package com.zufar.icedlatte.review.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.openapi.dto.ProductReviewRequest;
import com.zufar.icedlatte.review.messaging.kafka.inbox.ReviewCreatedInboxProcessor;
import com.zufar.icedlatte.review.messaging.kafka.outbox.ReviewCreatedKafkaPublisher;
import com.zufar.icedlatte.review.service.ProductReviewManager;
import com.zufar.icedlatte.test.config.AuthenticatedUserIntegrationSupport;
import com.zufar.icedlatte.user.api.UserLookupApi;

@DisplayName("Review Kafka database flow")
class ReviewKafkaDatabaseFlowIntegrationTest extends AuthenticatedUserIntegrationSupport {

    private static final String TOPIC = "iced-latte.review.created.v1";
    private static final UUID PRODUCT_ID = UUID.fromString("d1a2b3c4-0001-4000-8000-000000000007");
    private static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @Autowired
    private ProductReviewManager productReviewManager;

    @Autowired
    private ReviewCreatedKafkaPublisher outboxPublisher;

    @Autowired
    private ReviewCreatedInboxProcessor inboxProcessor;

    @Autowired
    private UserLookupApi userLookupApi;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) throws Exception {
        KAFKA.start();
        createTopic();

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("kafka.enabled", () -> "true");
        registry.add("kafka.topics.review-created", () -> TOPIC);
        registry.add("kafka.consumer-groups.review-ai", () -> "iced-latte-review-ai-e2e");
        registry.add("kafka.outbox.worker-id", () -> "review-e2e-outbox-worker");
        registry.add("kafka.inbox.worker-id", () -> "review-e2e-inbox-worker");
        registry.add("kafka.outbox.poll-interval", () -> "PT1H");
        registry.add("kafka.inbox.poll-interval", () -> "PT1H");
        registry.add("ai.enabled", () -> "false");
    }

    @Test
    @DisplayName("creates review, publishes outbox event, records inbox event, and processes it")
    void createsReviewPublishesOutboxRecordsInboxAndProcessesIt() {
        UUID userId = userLookupApi
                .getUserByEmail(registerAndAuthenticateUser().email())
                .id();
        ProductReviewRequest request = new ProductReviewRequest();
        request.setText("Testcontainers Kafka database flow review");
        request.setRating(5);

        ProductReviewDto createdReview = productReviewManager.create(PRODUCT_ID, userId, request);
        UUID reviewId = createdReview.getProductReviewId();

        UUID eventId = selectEventIdFromOutbox(reviewId);
        assertThat(selectOutboxStatus(eventId)).isEqualTo("PENDING");
        assertThat(selectOutboxPayload(eventId))
                .doesNotContain("Testcontainers Kafka database flow review", "\"text\"");

        startKafkaListeners();
        outboxPublisher.publishPendingOutboxEvents();

        waitUntil(() -> "PUBLISHED".equals(selectOutboxStatus(eventId)));
        waitUntil(() -> selectInboxStatus(eventId) != null);

        assertThat(selectInboxStatus(eventId)).isEqualTo("RECEIVED");
        assertThat(selectInboxPayload(eventId)).doesNotContain("Testcontainers Kafka database flow review", "\"text\"");

        inboxProcessor.processPendingInboxEvents();

        waitUntil(() -> "PROCESSED".equals(selectInboxStatus(eventId)));
    }

    private static void createTopic() throws Exception {
        Map<String, Object> bootstrapServersConfig =
                Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        try (AdminClient adminClient = AdminClient.create(bootstrapServersConfig)) {
            adminClient
                    .createTopics(List.of(new NewTopic(TOPIC, 1, (short) 1)))
                    .all()
                    .get();
        }
    }

    private UUID selectEventIdFromOutbox(UUID reviewId) {
        return jdbcTemplate.queryForObject("""
                SELECT event_id
                FROM outbox_events
                WHERE aggregate_id = ?
                  AND event_type = 'review.created'
                """, UUID.class, reviewId);
    }

    private String selectOutboxStatus(UUID eventId) {
        return jdbcTemplate.queryForObject("""
                SELECT status
                FROM outbox_events
                WHERE event_id = ?
                """, String.class, eventId);
    }

    private String selectOutboxPayload(UUID eventId) {
        return jdbcTemplate.queryForObject("""
                SELECT payload::text
                FROM outbox_events
                WHERE event_id = ?
                """, String.class, eventId);
    }

    private String selectInboxStatus(UUID eventId) {
        List<String> statuses = jdbcTemplate.queryForList("""
                SELECT status
                FROM inbox_events
                WHERE event_id = ?
                  AND consumer_name = 'iced-latte-review-ai-e2e'
                """, String.class, eventId);
        return statuses.isEmpty() ? null : statuses.getFirst();
    }

    private String selectInboxPayload(UUID eventId) {
        return jdbcTemplate.queryForObject("""
                SELECT payload::text
                FROM inbox_events
                WHERE event_id = ?
                  AND consumer_name = 'iced-latte-review-ai-e2e'
                """, String.class, eventId);
    }

    private void waitUntil(BooleanSupplier condition) {
        CompletableFuture<Void> completed = new CompletableFuture<>();
        try (ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor()) {
            executor.scheduleAtFixedRate(
                    () -> {
                        try {
                            if (condition.getAsBoolean()) {
                                completed.complete(null);
                            }
                        } catch (Exception e) {
                            completed.completeExceptionally(e);
                        }
                    },
                    0,
                    100,
                    TimeUnit.MILLISECONDS);
            completed.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new AssertionError("Timed out waiting for Kafka database flow", e);
        }
    }

    private void startKafkaListeners() {
        var listenerContainers = kafkaListenerEndpointRegistry.getAllListenerContainers();
        assertThat(listenerContainers).isNotEmpty();
        listenerContainers.forEach(container -> {
            if (!container.isRunning()) {
                container.start();
            }
        });
        waitUntil(() -> listenerContainers.stream().allMatch(org.springframework.context.Lifecycle::isRunning));
    }
}
