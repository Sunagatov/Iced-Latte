package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.common.config.PaginationConfig;
import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.openapi.dto.ProductReviewsAndRatingsWithPagination;
import com.zufar.icedlatte.review.converter.ProductReviewDtoConverter;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.validator.GetReviewsRequestValidator;
import com.zufar.icedlatte.review.validator.ProductReviewValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.zufar.icedlatte.review.converter.ProductReviewDtoConverter.EMPTY_PRODUCT_REVIEW_RESPONSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReviewsProvider unit tests")
class ProductReviewsProviderTest {

    @Mock private ProductReviewRepository reviewRepository;
    @Mock private ProductReviewDtoConverter productReviewDtoConverter;
    @Mock private ProductReviewValidator productReviewValidator;
    @Mock private GetReviewsRequestValidator getReviewsRequestValidator;
    @InjectMocks private ProductReviewsProvider provider;

    private UUID productId;
    private UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        var paginationConfig = new PaginationConfig();
        productId = UUID.randomUUID();
        userId = UUID.randomUUID();

        var field = ProductReviewsProvider.class.getDeclaredField("paginationConfig");
        field.setAccessible(true);
        field.set(provider, paginationConfig);
    }

    @Test
    @DisplayName("getProductReviews uses defaults when params are null")
    void getProductReviewsNullParamsUsesDefaults() {
        var page = new PageImpl<>(List.of(ProductReview.builder().id(UUID.randomUUID()).build()));
        when(reviewRepository.findAllProductReviews(eq(productId), eq(null), any(Pageable.class)))
                .thenReturn(page);
        var dto = new ProductReviewDto();
        when(productReviewDtoConverter.toProductReviewDto(any())).thenReturn(dto);
        var expected = new ProductReviewsAndRatingsWithPagination();
        when(productReviewDtoConverter.toProductReviewsAndRatingsWithPagination(any())).thenReturn(expected);

        var result = provider.getProductReviews(productId, null, null, null, null, null);

        assertThat(result).isEqualTo(expected);
        verify(productReviewValidator).validateProductExists(productId);
    }

    @Test
    @DisplayName("getProductReviewForUser returns empty response when no review found")
    void getProductReviewForUserNoReviewReturnsEmpty() {
        when(reviewRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());

        var result = provider.getProductReviewForUser(productId, userId);

        assertThat(result).isEqualTo(EMPTY_PRODUCT_REVIEW_RESPONSE);
        verify(productReviewValidator).validateProductExists(productId);
    }

    @Test
    @DisplayName("getProductReviewForUser returns mapped dto when review exists")
    void getProductReviewForUserReviewExistsReturnsMappedDto() {
        var review = ProductReview.builder().id(UUID.randomUUID()).build();
        var dto = new ProductReviewDto();
        when(reviewRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(review));
        when(productReviewDtoConverter.toProductReviewDto(review)).thenReturn(dto);

        assertThat(provider.getProductReviewForUser(productId, userId)).isEqualTo(dto);
    }

    @Test
    @DisplayName("getUserReviews uses current user id and returns paginated result")
    void getUserReviewsReturnsResult() {
        var page = new PageImpl<>(List.<ProductReview>of());
        when(reviewRepository.findAllByUserId(eq(userId), any(Pageable.class))).thenReturn(page);
        var expected = new ProductReviewsAndRatingsWithPagination();
        when(productReviewDtoConverter.toProductReviewsAndRatingsWithPagination(any())).thenReturn(expected);

        assertThat(provider.getUserReviews(userId, 0, 10, "createdAt", "desc")).isEqualTo(expected);
    }
}
