package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.exception.NotFoundException;
import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.review.converter.ProductReviewDtoConverter;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.entity.ProductReviewLike;
import com.zufar.icedlatte.review.repository.ProductReviewLikeRepository;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.validator.ProductReviewValidator;
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
    ProductReviewValidator productReviewValidator;
    @Mock
    ProductReviewRepository productReviewRepository;
    @Mock
    ProductReviewLikeRepository productReviewLikeRepository;
    @Mock
    ProductReviewDtoConverter productReviewDtoConverter;

    @Test
    @DisplayName("Should remove only the current user's vote when same vote is submitted again")
    void updateRemovesOnlyCurrentUserVote() {
        var productId = UUID.randomUUID();
        var reviewId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var productReview = ProductReview.builder()
                .id(reviewId).productId(productId).productRating(1).text("").createdAt(OffsetDateTime.now()).build();
        var existingLike = ProductReviewLike.builder()
                .userId(userId).productId(productId).productReviewId(reviewId).isLike(true).build();
        var expected = new ProductReviewDto(reviewId, productId, 1, "", OffsetDateTime.now(), "", "", 0, 0);

        when(productReviewLikeRepository.findByUserIdAndProductReviewId(userId, reviewId)).thenReturn(Optional.of(existingLike));
        when(productReviewRepository.findById(reviewId)).thenReturn(Optional.of(productReview));
        when(productReviewDtoConverter.toProductReviewDto(productReview)).thenReturn(expected);

        productReviewLikesUpdater.update(productId, reviewId, userId, true);

        verify(productReviewLikeRepository).deleteByUserIdAndProductReviewId(userId, reviewId);
    }

    @Test
    @DisplayName("Should update likes and return updated review DTO")
    void updateSuccessful() {
        var productId = UUID.randomUUID();
        var reviewId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var productReview = ProductReview.builder()
                .id(reviewId).productId(productId).productRating(1).text("").createdAt(OffsetDateTime.now()).build();
        var productReviewLike = ProductReviewLike.builder()
                .userId(userId).productId(productId).productReviewId(reviewId).isLike(true).build();
        var expected = new ProductReviewDto(reviewId, productId, 1, "", OffsetDateTime.now(), "", "", 0, 0);

        when(productReviewRepository.findById(reviewId)).thenReturn(Optional.of(productReview));
        when(productReviewLikeRepository.findByUserIdAndProductReviewId(userId, reviewId)).thenReturn(Optional.of(productReviewLike));
        when(productReviewDtoConverter.toProductReviewDto(productReview)).thenReturn(expected);

        assertEquals(expected, productReviewLikesUpdater.update(productId, reviewId, userId, true));

        verify(productReviewRepository).findById(reviewId);
        verify(productReviewDtoConverter).toProductReviewDto(productReview);
        verify(productReviewRepository).updateLikesCount(reviewId);
        verify(productReviewRepository).updateDislikesCount(reviewId);
    }

    @Test
    @DisplayName("Should throw NotFoundException when product does not exist")
    void updateFailsWhenProductNotFound() {
        var productId = UUID.randomUUID();
        var reviewId = UUID.randomUUID();
        var userId = UUID.randomUUID();

        doThrow(new NotFoundException("product not found"))
                .when(productReviewValidator)
                .validateProductIdIsValid(productId, reviewId);

        assertThrows(NotFoundException.class,
                () -> productReviewLikesUpdater.update(productId, reviewId, userId, true));
    }

    @Test
    @DisplayName("Should throw NotFoundException when review does not exist for user")
    void updateFailsWhenReviewNotFound() {
        var productId = UUID.randomUUID();
        var reviewId = UUID.randomUUID();
        var userId = UUID.randomUUID();

        doThrow(new NotFoundException("review not found"))
                .when(productReviewValidator).validateProductIdIsValid(productId, reviewId);

        assertThrows(NotFoundException.class,
                () -> productReviewLikesUpdater.update(productId, reviewId, userId, true));
    }

    @Test
    @DisplayName("Should throw BadRequestException when vote is null")
    void updateFailsWhenVoteIsNull() {
        var productId = UUID.randomUUID();
        var reviewId = UUID.randomUUID();
        var userId = UUID.randomUUID();

        assertThrows(BadRequestException.class,
                () -> productReviewLikesUpdater.update(productId, reviewId, userId, null));
    }
}
