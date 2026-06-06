package com.zufar.icedlatte.review.service.ai.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.exception.ReviewModerationException;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("LangChain4jReviewAiService unit tests")
class LangChain4jReviewAiServiceTest {

    @Mock
    private ReviewAiService reviewAiService;

    @Mock
    private ProductReviewRepository reviewRepository;

    @InjectMocks
    private LangChain4jReviewAiService service;

    @Test
    @DisplayName("moderate: passes when AI returns OK")
    void moderate_ok_doesNotThrow() {
        when(reviewAiService.moderate("Great coffee!")).thenReturn("OK");
        service.moderate("Great coffee!");
    }

    @Test
    @DisplayName("moderate: throws ReviewModerationException when AI returns NOT_OK")
    void moderate_notOk_throwsModerationException() {
        when(reviewAiService.moderate("spam text")).thenReturn("NOT_OK: spam detected");

        assertThatThrownBy(() -> service.moderate("spam text"))
                .isInstanceOf(ReviewModerationException.class)
                .hasMessageContaining("spam detected");
    }

    @Test
    @DisplayName("moderate: rejects malformed responses that only start with OK")
    void moderate_malformedOkPrefix_throwsModerationException() {
        when(reviewAiService.moderate("spam text")).thenReturn("OK_NOT_OK: spam detected");

        assertThatThrownBy(() -> service.moderate("spam text"))
                .isInstanceOf(ReviewModerationException.class)
                .hasMessageContaining("spam detected");
    }

    @Test
    @DisplayName("moderate: allows review through when AI is unavailable (fallback)")
    void moderate_aiUnavailable_doesNotThrow() {
        when(reviewAiService.moderate("some text")).thenThrow(new RuntimeException("timeout"));
        service.moderate("some text");
    }

    @Test
    @DisplayName("summarize: returns AI summary on success")
    void summarize_success_returnsSummary() {
        UUID productId = UUID.randomUUID();
        ProductReview review = mock(ProductReview.class);
        when(review.getText()).thenReturn("Loved it");
        when(reviewRepository.findAllByProductId(eq(productId), any(Pageable.class)))
                .thenReturn(List.of(review));
        when(reviewAiService.aggregateSummary(anyString())).thenReturn("A highly positive review praising the coffee.");

        var result = service.summarize(productId);

        assertThat(result).isEqualTo("A highly positive review praising the coffee.");
    }

    @Test
    @DisplayName("summarize: throws when AI is unavailable so caller can retry")
    void summarize_aiUnavailable_throwsForRetry() {
        UUID productId = UUID.randomUUID();
        ProductReview review = mock(ProductReview.class);
        when(review.getText()).thenReturn("Loved it");
        when(reviewRepository.findAllByProductId(eq(productId), any(Pageable.class)))
                .thenReturn(List.of(review));
        when(reviewAiService.aggregateSummary(anyString())).thenThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> service.summarize(productId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AI summary generation failed");
    }

    @Test
    @DisplayName("summarize: returns null when product has no reviews")
    void summarize_noReviews_returnsNull() {
        UUID productId = UUID.randomUUID();
        when(reviewRepository.findAllByProductId(eq(productId), any(Pageable.class)))
                .thenReturn(List.of());

        var result = service.summarize(productId);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("summarize: uses bounded first page sorted by newest reviews")
    void summarize_usesBoundedNewestReviews() {
        UUID productId = UUID.randomUUID();
        ProductReview review = mock(ProductReview.class);
        when(review.getText()).thenReturn("Loved it");
        when(reviewRepository.findAllByProductId(eq(productId), any(Pageable.class)))
                .thenReturn(List.of(review));
        when(reviewAiService.aggregateSummary(anyString())).thenReturn("Customers liked it.");

        service.summarize(productId);

        verify(reviewRepository).findAllByProductId(eq(productId), argThat(pageable -> {
            var createdAtOrder = pageable.getSort().getOrderFor("createdAt");
            if (createdAtOrder == null) {
                return false;
            }
            return pageable.getPageNumber() == 0 && pageable.getPageSize() == 100 && createdAtOrder.isDescending();
        }));
    }
}
