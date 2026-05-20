package com.zufar.icedlatte.review.internal;

import com.zufar.icedlatte.review.api.ReviewCreatedEvent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReviewCreatedEvent")
class ReviewCreatedEventTest {

    @Test
    @DisplayName("stores review id, text, and product id")
    void storesReviewIdTextAndProductId() {
        UUID reviewId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        ReviewCreatedEvent event = new ReviewCreatedEvent(reviewId, "Fresh review", productId);

        assertThat(event.reviewId()).isEqualTo(reviewId);
        assertThat(event.text()).isEqualTo("Fresh review");
        assertThat(event.productId()).isEqualTo(productId);
    }
}
