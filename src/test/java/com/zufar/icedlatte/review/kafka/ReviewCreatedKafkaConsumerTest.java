package com.zufar.icedlatte.review.kafka;

import com.zufar.icedlatte.review.ai.AsyncReviewProcessingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewCreatedKafkaConsumer")
class ReviewCreatedKafkaConsumerTest {

    @Mock
    private AsyncReviewProcessingService processingService;

    @InjectMocks
    private ReviewCreatedKafkaConsumer consumer;

    @Test
    @DisplayName("delegates consumed event to existing review processing service")
    void delegatesConsumedEventToExistingReviewProcessingService() {
        UUID eventId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ReviewCreatedKafkaEvent event = new ReviewCreatedKafkaEvent(
                eventId,
                "review.created",
                1,
                "iced-latte",
                Instant.parse("2026-05-18T12:00:00Z"),
                null,
                null,
                new ReviewCreatedKafkaEvent.Payload(reviewId, productId, "Great coffee")
        );

        consumer.consume(event);

        var captor = ArgumentCaptor.forClass(com.zufar.icedlatte.review.api.ReviewCreatedEvent.class);
        verify(processingService).process(captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo(eventId);
        assertThat(captor.getValue().reviewId()).isEqualTo(reviewId);
        assertThat(captor.getValue().productId()).isEqualTo(productId);
        assertThat(captor.getValue().text()).isEqualTo("Great coffee");
    }
}
