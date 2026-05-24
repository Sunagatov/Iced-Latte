package com.zufar.icedlatte.review.kafka;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.zufar.icedlatte.review.dto.ReviewCreatedEvent;
import com.zufar.icedlatte.review.messaging.kafka.event.ReviewCreatedKafkaEvent;

@DisplayName("ReviewCreatedKafkaEvent")
class ReviewCreatedKafkaEventTest {

    @Test
    @DisplayName("maps domain review-created event into versioned Kafka envelope")
    void mapsDomainEventIntoVersionedKafkaEnvelope() {
        UUID eventId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-05-18T12:00:00Z");
        ReviewCreatedEvent domainEvent =
                new ReviewCreatedEvent(eventId, reviewId, "Fresh review", productId, occurredAt);

        ReviewCreatedKafkaEvent kafkaEvent = ReviewCreatedKafkaEvent.fromDomainEvent(domainEvent);

        assertThat(kafkaEvent.eventId()).isEqualTo(eventId);
        assertThat(kafkaEvent.eventType()).isEqualTo("review.created");
        assertThat(kafkaEvent.eventVersion()).isEqualTo(1);
        assertThat(kafkaEvent.sourceApp()).isEqualTo("iced-latte");
        assertThat(kafkaEvent.occurredAt()).isEqualTo(occurredAt);
        assertThat(kafkaEvent.payload().reviewId()).isEqualTo(reviewId);
        assertThat(kafkaEvent.payload().productId()).isEqualTo(productId);
    }

    @Test
    @DisplayName("does not put review text in Kafka payload")
    void doesNotPutReviewTextInKafkaPayload() {
        UUID reviewId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ReviewCreatedEvent domainEvent = new ReviewCreatedEvent(reviewId, "Fresh review", productId);

        ReviewCreatedKafkaEvent kafkaEvent = ReviewCreatedKafkaEvent.fromDomainEvent(domainEvent);

        assertThat(kafkaEvent.payload().reviewId()).isEqualTo(reviewId);
        assertThat(kafkaEvent.payload().productId()).isEqualTo(productId);
        assertThat(ReviewCreatedKafkaEvent.Payload.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .doesNotContain("text");
    }

    @Test
    @DisplayName("serialized envelope matches the review-created JSON schema")
    void serializedEnvelopeMatchesReviewCreatedJsonSchema() throws Exception {
        UUID reviewId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ReviewCreatedEvent domainEvent = new ReviewCreatedEvent(
                UUID.randomUUID(), reviewId, "Fresh review", productId, Instant.parse("2026-05-24T12:00:00Z"));
        ReviewCreatedKafkaEvent kafkaEvent = ReviewCreatedKafkaEvent.fromDomainEvent(domainEvent);
        ObjectMapper objectMapper =
                new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String json = objectMapper.writeValueAsString(kafkaEvent);

        org.hamcrest.MatcherAssert.assertThat(
                json, matchesJsonSchema(new File("docs/events/schemas/review-created-event.schema.json")));
        assertThat(json).doesNotContain("Fresh review", "\"text\"");
    }
}
