package com.zufar.icedlatte.review.messaging.kafka.inbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.review.messaging.kafka.config.KafkaIntegrationProperties;
import com.zufar.icedlatte.review.messaging.kafka.event.ReviewCreatedKafkaEvent;
import com.zufar.icedlatte.review.service.ai.AsyncReviewProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewCreatedInboxProcessor")
class ReviewCreatedInboxProcessorTest {

    @Mock
    private InboxEventRepository inboxEventRepository;

    @Mock
    private AsyncReviewProcessingService processingService;

    private ObjectMapper objectMapper;
    private ReviewCreatedInboxProcessor processor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        KafkaIntegrationProperties properties = new KafkaIntegrationProperties(
                true,
                new KafkaIntegrationProperties.Topics("iced-latte.review.created.v1"),
                new KafkaIntegrationProperties.ConsumerGroups("iced-latte-review-ai"),
                KafkaIntegrationProperties.Outbox.defaults(),
                new KafkaIntegrationProperties.Inbox(true, true, 25, 10, Duration.ofSeconds(5),
                        Duration.ofMinutes(5), "test-inbox-worker")
        );
        processor = new ReviewCreatedInboxProcessor(objectMapper, properties, inboxEventRepository, processingService);
    }

    @Test
    @DisplayName("claims only rows owned by the review AI consumer")
    void claimsOnlyRowsOwnedByReviewAiConsumer() {
        when(inboxEventRepository.claimProcessableEvents(25, "iced-latte-review-ai", "test-inbox-worker"))
                .thenReturn(List.of());

        processor.processPendingInboxEvents();

        verify(inboxEventRepository)
                .claimProcessableEvents(25, "iced-latte-review-ai", "test-inbox-worker");
    }

    @Test
    @DisplayName("marks missing review events as ignored")
    void marksMissingReviewEventsAsIgnored() throws Exception {
        UUID rowId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(reviewCreatedEvent(eventId, reviewId));
        when(inboxEventRepository.claimProcessableEvents(25, "iced-latte-review-ai", "test-inbox-worker"))
                .thenReturn(List.of(new InboxEventRepository.InboxEventRow(rowId, eventId, payload, 0, 10)));
        when(processingService.processByReviewId(reviewId))
                .thenReturn(AsyncReviewProcessingService.ProcessingResult.IGNORED);

        processor.processPendingInboxEvents();

        verify(inboxEventRepository).markIgnored(rowId, "test-inbox-worker");
    }

    @Test
    @DisplayName("marks processing failures retryable or permanent through repository failure handling")
    void marksProcessingFailuresThroughRepository() throws Exception {
        UUID rowId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        String payload = objectMapper.writeValueAsString(reviewCreatedEvent(eventId, reviewId));
        IllegalStateException failure = new IllegalStateException("moderation unavailable");
        when(inboxEventRepository.claimProcessableEvents(25, "iced-latte-review-ai", "test-inbox-worker"))
                .thenReturn(List.of(new InboxEventRepository.InboxEventRow(rowId, eventId, payload, 2, 10)));
        when(processingService.processByReviewId(reviewId))
                .thenThrow(failure);

        processor.processPendingInboxEvents();

        verify(inboxEventRepository)
                .markFailed(rowId, "test-inbox-worker", 2, 10, failure);
    }

    private ReviewCreatedKafkaEvent reviewCreatedEvent(UUID eventId, UUID reviewId) {
        return new ReviewCreatedKafkaEvent(
                eventId,
                "review.created",
                1,
                "iced-latte",
                Instant.parse("2026-05-24T12:00:00Z"),
                null,
                null,
                new ReviewCreatedKafkaEvent.Payload(reviewId, UUID.randomUUID())
        );
    }
}
