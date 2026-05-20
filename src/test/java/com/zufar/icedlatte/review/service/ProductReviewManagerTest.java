package com.zufar.icedlatte.review.service;

import com.zufar.icedlatte.review.api.ReviewCreatedEvent;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.openapi.dto.ProductReviewRequest;
import com.zufar.icedlatte.product.api.ProductReviewProductGateway;
import com.zufar.icedlatte.review.converter.ProductReviewDtoConverter;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.validator.ProductReviewValidator;
import com.zufar.icedlatte.user.service.SingleUserProvider;
import com.zufar.icedlatte.user.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReviewManager unit tests")
class ProductReviewManagerTest {

    @Mock private ProductReviewRepository reviewRepository;
    @Mock private ProductReviewDtoConverter productReviewDtoConverter;
    @Mock private SingleUserProvider singleUserProvider;
    @Mock private ProductReviewValidator productReviewValidator;
    @Mock private ProductReviewProductGateway productReviewProductGateway;
    @Mock private com.zufar.icedlatte.review.ai.summary.ProductReviewSummaryDebouncer summaryDebouncer;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ProductReviewManager service;

    @BeforeEach
    void setUp() {
        service = new ProductReviewManager(
                reviewRepository, productReviewDtoConverter, singleUserProvider,
                productReviewValidator, productReviewProductGateway, summaryDebouncer, eventPublisher
        );
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Creates review, trims text, updates stats, returns DTO")
        void create_validRequest_savesAndReturnsDto() {
            UUID userId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UserEntity user = UserEntity.builder().id(userId).build();
            ProductReviewRequest request = new ProductReviewRequest();
            request.setText("  Great coffee!  ");
            request.setRating(5);
            ProductReviewDto expectedDto = new ProductReviewDto();

            when(singleUserProvider.getUserEntityById(userId)).thenReturn(user);
            UUID generatedId = UUID.randomUUID();
            doAnswer(invocation -> {
                ProductReview review = invocation.getArgument(0);
                review.setId(generatedId);
                return review;
            }).when(reviewRepository).saveAndFlush(any(ProductReview.class));
            when(productReviewDtoConverter.toProductReviewDto(any())).thenReturn(expectedDto);

            ProductReviewDto result = service.create(productId, userId, request);

            assertThat(result).isEqualTo(expectedDto);

            ArgumentCaptor<ProductReview> captor = ArgumentCaptor.forClass(ProductReview.class);
            verify(reviewRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getText()).isEqualTo("Great coffee!");
            assertThat(captor.getValue().getProductRating()).isEqualTo(5);
            assertThat(captor.getValue().getLikesCount()).isZero();
            assertThat(captor.getValue().getDislikesCount()).isZero();

            verify(summaryDebouncer).schedule(productId);
            verify(productReviewProductGateway).refreshReviewAggregates(productId);
            ArgumentCaptor<ReviewCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ReviewCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().text()).isEqualTo("Great coffee!");
            assertThat(eventCaptor.getValue().productId()).isEqualTo(productId);
        }

        @Test
        @DisplayName("Propagates BadRequestException from validator")
        void create_emptyText_throwsBadRequestException() {
            UUID userId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            ProductReviewRequest request = new ProductReviewRequest();
            request.setText("   ");
            request.setRating(3);

            doThrow(new BadRequestException("Product's review is empty")).when(productReviewValidator).validateReviewText("   ");

            assertThatThrownBy(() -> service.create(productId, userId, request))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("Deletes review and updates product stats")
        void delete_validRequest_deletesAndUpdatesStats() {
            UUID productId = UUID.randomUUID();
            UUID reviewId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            service.delete(productId, reviewId, userId);

            verify(productReviewValidator).validateProductReviewDeletionAllowed(reviewId, userId);
            verify(productReviewValidator).validateProductIdIsValid(productId, reviewId);
            verify(reviewRepository).deleteById(reviewId);
            verify(productReviewProductGateway).refreshReviewAggregates(productId);
            verify(summaryDebouncer).schedule(productId);
        }

        @Test
        @DisplayName("Propagates BadRequestException when deletion not allowed")
        void delete_notOwner_throwsBadRequestException() {
            UUID productId = UUID.randomUUID();
            UUID reviewId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            doThrow(new BadRequestException("Deletion denied"))
                    .when(productReviewValidator).validateProductReviewDeletionAllowed(reviewId, userId);

            assertThatThrownBy(() -> service.delete(productId, reviewId, userId))
                    .isInstanceOf(BadRequestException.class);
        }
    }
}
