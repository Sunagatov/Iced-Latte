package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.openapi.dto.ProductReviewRatingStats;
import com.zufar.icedlatte.openapi.dto.RatingMap;
import com.zufar.icedlatte.review.converter.ProductReviewDtoConverter;
import com.zufar.icedlatte.review.dto.ProductRatingCount;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.validator.ProductReviewValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReviewsStatisticsProvider Tests")
class ProductReviewsStatisticsProviderTest {

    @InjectMocks
    ProductReviewsStatisticsProvider productReviewsStatisticsProvider;

    @Mock
    ProductReviewRepository reviewRepository;

    @Mock
    ProductReviewDtoConverter productReviewDtoConverter;

    @Mock
    ProductReviewValidator productReviewValidator;

    @Test
    @DisplayName("Should return stats with correct avg rating and rating map")
    void shouldReturnStatsWithCorrectAvgRatingAndRatingMap() {
        UUID productId = UUID.randomUUID();
        List<ProductRatingCount> ratingCounts = List.of(new ProductRatingCount(1, 1));
        RatingMap expectedRatingMap = new RatingMap(1, 0, 0, 0, 0);

        when(reviewRepository.getAvgRatingByProductId(productId)).thenReturn(1.0);
        when(reviewRepository.getReviewCountProductById(productId)).thenReturn(1);
        when(reviewRepository.getRatingsMapByProductId(productId)).thenReturn(ratingCounts);
        when(productReviewDtoConverter.convertToProductRatingMap(ratingCounts)).thenReturn(expectedRatingMap);

        ProductReviewRatingStats result = productReviewsStatisticsProvider.get(productId);

        assertEquals(productId, result.getProductId());
        assertEquals(1.0, result.getAvgRating());
        assertEquals(1, result.getReviewsCount());
        assertEquals(expectedRatingMap, result.getRatingMap());

        verify(productReviewValidator).validateProductExists(productId);
        verify(reviewRepository).getAvgRatingByProductId(productId);
        verify(reviewRepository).getReviewCountProductById(productId);
        verify(reviewRepository).getRatingsMapByProductId(productId);
        verify(productReviewDtoConverter).convertToProductRatingMap(ratingCounts);
    }

    @Test
    @DisplayName("Should return zero avg rating when repository returns null")
    void shouldReturnZeroAvgRatingWhenRepositoryReturnsNull() {
        UUID productId = UUID.randomUUID();
        List<ProductRatingCount> ratingCounts = List.of();
        RatingMap emptyRatingMap = new RatingMap();

        when(reviewRepository.getAvgRatingByProductId(productId)).thenReturn(null);
        when(reviewRepository.getReviewCountProductById(productId)).thenReturn(0);
        when(reviewRepository.getRatingsMapByProductId(productId)).thenReturn(ratingCounts);
        when(productReviewDtoConverter.convertToProductRatingMap(ratingCounts)).thenReturn(emptyRatingMap);

        ProductReviewRatingStats result = productReviewsStatisticsProvider.get(productId);

        assertEquals(0.0, result.getAvgRating());
        assertEquals(0, result.getReviewsCount());

        verify(productReviewValidator).validateProductExists(productId);
    }

    @Test
    @DisplayName("Should return empty rating map when repository returns null rating counts")
    void shouldReturnEmptyRatingMapWhenRepositoryReturnsNullRatingCounts() {
        UUID productId = UUID.randomUUID();

        when(reviewRepository.getAvgRatingByProductId(productId)).thenReturn(0.0);
        when(reviewRepository.getReviewCountProductById(productId)).thenReturn(0);
        when(reviewRepository.getRatingsMapByProductId(productId)).thenReturn(null);

        ProductReviewRatingStats result = productReviewsStatisticsProvider.get(productId);

        assertEquals(new RatingMap(), result.getRatingMap());
        verify(productReviewValidator).validateProductExists(productId);
    }
}
