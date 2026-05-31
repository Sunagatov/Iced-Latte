package com.zufar.icedlatte.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.zufar.icedlatte.common.config.PaginationConfig;
import com.zufar.icedlatte.common.exception.NotFoundException;
import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.openapi.dto.ProductReviewsAndRatingsWithPagination;
import com.zufar.icedlatte.review.converter.ProductReviewDtoConverter;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.service.validator.GetReviewsRequestValidator;
import com.zufar.icedlatte.review.service.validator.ProductReviewValidator;
import com.zufar.icedlatte.user.api.UserLookupApi;
import com.zufar.icedlatte.user.api.dto.UserLookupSnapshot;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReviewsProvider unit tests")
class ProductReviewsProviderTest {

    @Mock
    private ProductReviewRepository reviewRepository;

    @Mock
    private ProductReviewDtoConverter productReviewDtoConverter;

    @Mock
    private ProductReviewValidator productReviewValidator;

    @Mock
    private GetReviewsRequestValidator getReviewsRequestValidator;

    @Mock
    private UserLookupApi userLookupApi;

    @InjectMocks
    private ProductReviewsProvider provider;

    private UUID productId;
    private UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        var paginationConfig = new PaginationConfig(
                0,
                new PaginationConfig.Products(50, "name", "desc"),
                new PaginationConfig.Reviews(10, "createdAt", "desc"),
                new PaginationConfig.Orders(10, 50, "createdAt", "desc"));
        productId = UUID.randomUUID();
        userId = UUID.randomUUID();

        var paginationConfigField = ProductReviewsProvider.class.getDeclaredField("paginationConfig");
        paginationConfigField.setAccessible(true);
        paginationConfigField.set(provider, paginationConfig);
    }

    @Test
    @DisplayName("getProductReviews uses defaults when params are null")
    void getProductReviewsNullParamsUsesDefaults() {
        var review =
                ProductReview.builder().id(UUID.randomUUID()).userId(userId).build();
        var page = new PageImpl<>(List.of(review));
        when(reviewRepository.findAllProductReviews(eq(productId), eq(null), any(Pageable.class)))
                .thenReturn(page);
        var user = user();
        when(userLookupApi.getUsersByIds(Set.of(userId))).thenReturn(Set.of(user));
        var dto = new ProductReviewDto();
        when(productReviewDtoConverter.toProductReviewDto(review, user)).thenReturn(dto);
        var expected = new ProductReviewsAndRatingsWithPagination();
        when(productReviewDtoConverter.toProductReviewsAndRatingsWithPagination(any()))
                .thenReturn(expected);

        var result = provider.getProductReviews(productId, null, null, null, null, null);

        assertThat(result).isEqualTo(expected);
        verify(productReviewValidator).validateProductExists(productId);
        verify(getReviewsRequestValidator).validate(0, 10, "createdAt", "desc", null);
        verify(userLookupApi, never()).getUserById(userId);
    }

    @Test
    @DisplayName("getProductReviewForUser throws NotFoundException when no review found")
    void getProductReviewForUserNoReviewReturnsEmpty() {
        when(reviewRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.getProductReviewForUser(productId, userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(productId.toString())
                .hasMessageContaining(userId.toString());
        verify(productReviewValidator).validateProductExists(productId);
    }

    @Test
    @DisplayName("getProductReviewForUser returns mapped dto when review exists")
    void getProductReviewForUserReviewExistsReturnsMappedDto() {
        var review =
                ProductReview.builder().id(UUID.randomUUID()).userId(userId).build();
        var user = user();
        var dto = new ProductReviewDto();
        when(reviewRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(review));
        when(userLookupApi.getUserById(userId)).thenReturn(user);
        when(productReviewDtoConverter.toProductReviewDto(review, user)).thenReturn(dto);

        assertThat(provider.getProductReviewForUser(productId, userId)).isEqualTo(dto);
    }

    @Test
    @DisplayName("getUserReviews uses current user id and returns paginated result")
    void getUserReviewsReturnsResult() {
        var page = new PageImpl<>(List.<ProductReview>of());
        when(reviewRepository.findAllByUserId(eq(userId), any(Pageable.class))).thenReturn(page);
        var expected = new ProductReviewsAndRatingsWithPagination();
        when(productReviewDtoConverter.toProductReviewsAndRatingsWithPagination(any()))
                .thenReturn(expected);

        assertThat(provider.getUserReviews(userId, 0, 10, "createdAt", "desc")).isEqualTo(expected);
        verify(getReviewsRequestValidator).validate(0, 10, "createdAt", "desc", null);
    }

    private UserLookupSnapshot user() {
        return new UserLookupSnapshot(userId, "Ada", "Lovelace", "ada@example.com");
    }
}
