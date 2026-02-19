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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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

        verify(securityPrincipalProvider, times(1)).getUserId();
        verify(productReviewProvider, times(1)).getReviewEntityById(reviewId);
        verify(productReviewDtoConverter, times(1)).toProductReviewDto(productReview);
        verify(productReviewRepository, times(1)).updateLikesCount(reviewId);
        verify(productReviewRepository, times(1)).updateDislikesCount(reviewId);
    }

    @Test
    void updateFailsWhenValidateProductExistsFails() {
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
        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        doThrow(new ProductNotFoundForReviewException(productId))
                .when(productReviewValidator)
                .validateProductIdIsValid(productId, reviewId);

        assertThrows(ProductNotFoundForReviewException.class, () -> productReviewLikesUpdater.update(productId, reviewId, true));

        verify(securityPrincipalProvider, times(1)).getUserId();
        verify(productReviewProvider, times(0)).getReviewEntityById(reviewId);
        verify(productReviewDtoConverter, times(0)).toProductReviewDto(productReview);
        verify(productReviewRepository, times(0)).updateLikesCount(reviewId);
        verify(productReviewRepository, times(0)).updateDislikesCount(reviewId);
    }

    @Test
    void updateFailsWhenValidateReviewExistsForUser() {
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
        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        doThrow(new ProductReviewNotFoundException(productId))
                .when(productReviewValidator).validateProductIdIsValid(productId, reviewId);

        assertThrows(ProductReviewNotFoundException.class, () -> productReviewLikesUpdater.update(productId, reviewId, true));

        verify(securityPrincipalProvider, times(1)).getUserId();
        verify(productReviewProvider, times(0)).getReviewEntityById(reviewId);
        verify(productReviewDtoConverter, times(0)).toProductReviewDto(productReview);
        verify(productReviewRepository, times(0)).updateLikesCount(reviewId);
        verify(productReviewRepository, times(0)).updateDislikesCount(reviewId);
    }

    @Test
    void updateFailsWhenValidateProductIdIsValid() {
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
        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        doThrow(new ProductNotFoundForReviewException(productId))
                .when(productReviewValidator).validateProductIdIsValid(productId, reviewId);

        assertThrows(ProductNotFoundForReviewException.class, () -> productReviewLikesUpdater.update(productId, reviewId, true));

        verify(securityPrincipalProvider, times(1)).getUserId();
        verify(productReviewProvider, times(0)).getReviewEntityById(reviewId);
        verify(productReviewDtoConverter, times(0)).toProductReviewDto(productReview);
        verify(productReviewRepository, times(0)).updateLikesCount(reviewId);
        verify(productReviewRepository, times(0)).updateDislikesCount(reviewId);
    }
}
