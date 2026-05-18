package com.zufar.icedlatte.review.kafka;

import com.zufar.icedlatte.review.api.ReviewCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewCreatedKafkaPublisher")
class ReviewCreatedKafkaPublisherTest {

    @Mock
    private KafkaTemplate<String, ReviewCreatedKafkaEvent> kafkaTemplate;

    private ReviewCreatedKafkaPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new ReviewCreatedKafkaPublisher(kafkaTemplate);
        ReflectionTestUtils.setField(publisher, "reviewCreatedTopic", "iced-latte.review.created.v1");
    }

    @Test
    @DisplayName("publishes review-created envelope keyed by product id")
    void publishesReviewCreatedEnvelopeKeyedByProductId() {
        UUID eventId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ReviewCreatedEvent event = new ReviewCreatedEvent(
                eventId,
                reviewId,
                "Great coffee",
                productId,
                Instant.parse("2026-05-18T12:00:00Z")
        );
        when(kafkaTemplate.send(eq("iced-latte.review.created.v1"), eq(productId.toString()), any(ReviewCreatedKafkaEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish(event);

        ArgumentCaptor<ReviewCreatedKafkaEvent> eventCaptor = ArgumentCaptor.forClass(ReviewCreatedKafkaEvent.class);
        verify(kafkaTemplate).send(eq("iced-latte.review.created.v1"), eq(productId.toString()), eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventId()).isEqualTo(eventId);
        assertThat(eventCaptor.getValue().payload().reviewId()).isEqualTo(reviewId);
    }
}
