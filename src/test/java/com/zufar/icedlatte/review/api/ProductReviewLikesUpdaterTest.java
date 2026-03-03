package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.review.converter.ProductReviewDtoConverter;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.entity.ProductReviewLike;
import com.zufar.icedlatte.review.exception.ProductNotFoundForReviewException;
import com.zufar.icedlatte.review.exception.ProductReviewNotFoundException;
import com.zufar.icedlatte.review.repository.ProductReviewLikeRepository;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.validator.ProductReviewValidator;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReviewLikesUpdater Tests")
class ProductReviewLikesUpdaterTest {

    @InjectMocks
    ProductReviewLikesUpdater productReviewLikesUpdater;

    @Mock
    SecurityPrincipalProvider securityPrincipalProvider;
    @Mock
    ProductReviewValidator productReviewValidator;
    @Mock
    ProductReviewRepository productReviewRepository;
    @Mock
    ProductReviewLikeRepository productReviewLikeRepository;
    @Mock
    ProductReviewProvider productReviewProvider;
    @Mock
    ProductReviewDtoConverter productReviewDtoConverter;

    @Test
    @DisplayName("Should update likes and return updated review DTO")
    void updateSuccessful() {
        var productId = UUID.randomUUID();
        var reviewId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var productReview = ProductReview.builder()
                .id(reviewId)
                .productId(productId)
                .productRating(1)
                .text("")
                .createdAt(OffsetDateTime.now())
                .build();
        var productReviewLike = ProductReviewLike.builder()
                .userId(userId)
                .productId(productId)
                .productReviewId(reviewId)
                .isLike(true)
                .build();
        var expected = new ProductReviewDto(reviewId, productId, 1, "", OffsetDateTime.now(),
                "", "", 0, 0);

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(productReviewProvider.getReviewEntityById(reviewId)).thenReturn(productReview);
        when(productReviewLikeRepository.findByUserIdAndProductReviewId(userId, reviewId)).thenReturn(Optional.of(productReviewLike));
        when(productReviewDtoConverter.toProductReviewDto(productReview)).thenReturn(expected);

        assertEquals(expected, productReviewLikesUpdater.update(productId, reviewId, true));

        verify(securityPrincipalProvider).getUserId();
        verify(productReviewProvider).getReviewEntityById(reviewId);
        verify(productReviewDtoConverter).toProductReviewDto(productReview);
        verify(productReviewRepository).updateLikesCount(reviewId);
        verify(productReviewRepository).updateDislikesCount(reviewId);
    }

    @Test
    @DisplayName("Should throw ProductNotFoundForReviewException when product does not exist")
    void updateFailsWhenProductNotFound() {
        var productId = UUID.randomUUID();
        var reviewId = UUID.randomUUID();
        var userId = UUID.randomUUID();

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        doThrow(new ProductNotFoundForReviewException(productId))
                .when(productReviewValidator)
                .validateProductIdIsValid(productId, reviewId);

        assertThrows(ProductNotFoundForReviewException.class, () -> productReviewLikesUpdater.update(productId, reviewId, true));

        verify(securityPrincipalProvider).getUserId();
    }

    @Test
    @DisplayName("Should throw ProductReviewNotFoundException when review does not exist for user")
    void updateFailsWhenReviewNotFound() {
        var productId = UUID.randomUUID();
        var reviewId = UUID.randomUUID();
        var userId = UUID.randomUUID();

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        doThrow(new ProductReviewNotFoundException(productId))
                .when(productReviewValidator).validateProductIdIsValid(productId, reviewId);

        assertThrows(ProductReviewNotFoundException.class, () -> productReviewLikesUpdater.update(productId, reviewId, true));

        verify(securityPrincipalProvider).getUserId();
    }
}
