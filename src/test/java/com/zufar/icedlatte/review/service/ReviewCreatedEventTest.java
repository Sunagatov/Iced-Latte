package com.zufar.icedlatte.review.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.review.dto.ReviewCreatedEvent;

@DisplayName("ReviewCreatedEvent")
class ReviewCreatedEventTest {

    @Test
    @DisplayName("stores review id and product id")
    void storesReviewIdAndProductId() {
        UUID reviewId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        ReviewCreatedEvent event = new ReviewCreatedEvent(reviewId, productId);

        assertThat(event.reviewId()).isEqualTo(reviewId);
        assertThat(event.productId()).isEqualTo(productId);
    }
}
