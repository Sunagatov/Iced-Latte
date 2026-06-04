package com.zufar.icedlatte.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.openapi.dto.ProductReviewRequest;
import com.zufar.icedlatte.product.api.ProductReviewProductApi;
import com.zufar.icedlatte.review.converter.ProductReviewDtoConverter;
import com.zufar.icedlatte.review.dto.ReviewCreatedEvent;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.repository.ProductReviewLikeRepository;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.service.ai.summary.ProductReviewSummaryDebouncer;
import com.zufar.icedlatte.review.service.validator.ProductReviewValidator;
import com.zufar.icedlatte.security.signin.turnstile.TurnstileProperties;
import com.zufar.icedlatte.security.signin.turnstile.TurnstileVerifier;
import com.zufar.icedlatte.user.api.UserLookupApi;
import com.zufar.icedlatte.user.api.dto.UserLookupSnapshot;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReviewManager unit tests")
class ProductReviewManagerTest {

    @Mock
    private ProductReviewRepository reviewRepository;

    @Mock
    private ProductReviewLikeRepository productReviewLikeRepository;

    @Mock
    private ProductReviewDtoConverter productReviewDtoConverter;

    @Mock
    private UserLookupApi userLookupApi;

    @Mock
    private ProductReviewValidator productReviewValidator;

    @Mock
    private ProductReviewProductApi productReviewProductGateway;

    @Mock
    private ProductReviewSummaryDebouncer summaryDebouncer;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TurnstileVerifier turnstileVerifier;

    @Mock
    private TurnstileProperties turnstileProperties;

    private ProductReviewManager service;

    @BeforeEach
    void setUp() {
        service = new ProductReviewManager(
                reviewRepository,
                productReviewLikeRepository,
                productReviewDtoConverter,
                userLookupApi,
                productReviewValidator,
                productReviewProductGateway,
                summaryDebouncer,
                eventPublisher,
                turnstileVerifier,
                turnstileProperties);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Creates review, trims text, updates stats, returns DTO")
        void create_validRequest_savesAndReturnsDto() {
            UUID userId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            ProductReviewRequest request = new ProductReviewRequest();
            request.setText("  Great coffee!  ");
            request.setRating(5);
            ProductReviewDto expectedDto = new ProductReviewDto();

            var user = new UserLookupSnapshot(userId, "Ada", "Lovelace", "ada@example.com");
            when(userLookupApi.getUserById(userId)).thenReturn(user);
            UUID generatedId = UUID.randomUUID();
            doAnswer(invocation -> {
                        ProductReview review = invocation.getArgument(0);
                        review.setId(generatedId);
                        return review;
                    })
                    .when(reviewRepository)
                    .saveAndFlush(any(ProductReview.class));
            when(productReviewDtoConverter.toProductReviewDto(any(), eq(user))).thenReturn(expectedDto);

            ProductReviewDto result = service.create(productId, userId, request);

            assertThat(result).isEqualTo(expectedDto);

            ArgumentCaptor<ProductReview> captor = ArgumentCaptor.forClass(ProductReview.class);
            verify(reviewRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(userId);
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
            verifyNoInteractions(turnstileVerifier);
        }

        @Test
        @DisplayName("Verifies Turnstile token when review protection is enabled")
        void create_reviewsTurnstileEnabled_verifiesToken() {
            when(turnstileProperties.reviewsEnabled()).thenReturn(true);
            UUID userId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            ProductReviewRequest request = new ProductReviewRequest();
            request.setText("Great coffee!");
            request.setRating(5);
            request.setTurnstileToken("turnstile-token");
            ProductReviewDto expectedDto = new ProductReviewDto();

            var user = new UserLookupSnapshot(userId, "Ada", "Lovelace", "ada@example.com");
            when(userLookupApi.getUserById(userId)).thenReturn(user);
            doAnswer(invocation -> {
                        ProductReview review = invocation.getArgument(0);
                        review.setId(UUID.randomUUID());
                        return review;
                    })
                    .when(reviewRepository)
                    .saveAndFlush(any(ProductReview.class));
            when(productReviewDtoConverter.toProductReviewDto(any(), eq(user))).thenReturn(expectedDto);

            ProductReviewDto result = service.create(productId, userId, request);

            assertThat(result).isEqualTo(expectedDto);
            verify(turnstileVerifier).verify("turnstile-token");
            verify(reviewRepository).saveAndFlush(any(ProductReview.class));
        }

        @Test
        @DisplayName("Stops before saving when enabled Turnstile verification fails")
        void create_reviewsTurnstileEnabledVerificationFails_doesNotSave() {
            when(turnstileProperties.reviewsEnabled()).thenReturn(true);
            UUID userId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            ProductReviewRequest request = new ProductReviewRequest();
            request.setText("Great coffee!");
            request.setRating(5);
            request.setTurnstileToken("bad-token");
            doThrow(new BadRequestException("Turnstile verification failed"))
                    .when(turnstileVerifier)
                    .verify("bad-token");

            assertThatThrownBy(() -> service.create(productId, userId, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Turnstile verification failed");

            verify(turnstileVerifier).verify("bad-token");
            verifyNoInteractions(productReviewValidator, userLookupApi, reviewRepository);
        }

        @Test
        @DisplayName("Propagates BadRequestException from validator")
        void create_emptyText_throwsBadRequestException() {
            UUID userId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            ProductReviewRequest request = new ProductReviewRequest();
            request.setText("   ");
            request.setRating(3);

            doThrow(new BadRequestException("Product's review is empty"))
                    .when(productReviewValidator)
                    .validateReviewText("   ");

            assertThatThrownBy(() -> service.create(productId, userId, request))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Rejects missing request")
        void create_missingRequest_throwsBadRequestException() {
            assertThatThrownBy(() -> service.create(UUID.randomUUID(), UUID.randomUUID(), null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Product's review request must be provided");
        }

        @Test
        @DisplayName("Validates rating before saving")
        void create_invalidRating_throwsBadRequestExceptionBeforeSave() {
            UUID userId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            ProductReviewRequest request = new ProductReviewRequest();
            request.setText("Great coffee");
            request.setRating(null);
            doThrow(new BadRequestException("Product's review rating must be between 1 and 5"))
                    .when(productReviewValidator)
                    .validateProductRating(null);

            assertThatThrownBy(() -> service.create(productId, userId, request))
                    .isInstanceOf(BadRequestException.class);

            verify(reviewRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Translates duplicate review persistence race to BadRequestException")
        void create_duplicateReviewRace_throwsBadRequestException() {
            UUID userId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            ProductReviewRequest request = new ProductReviewRequest();
            request.setText("Great coffee");
            request.setRating(5);
            var user = new UserLookupSnapshot(userId, "Ada", "Lovelace", "ada@example.com");
            when(userLookupApi.getUserById(userId)).thenReturn(user);
            when(reviewRepository.saveAndFlush(any(ProductReview.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate"));

            assertThatThrownBy(() -> service.create(productId, userId, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Creation of the product's review");
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
                    .when(productReviewValidator)
                    .validateProductReviewDeletionAllowed(reviewId, userId);

            assertThatThrownBy(() -> service.delete(productId, reviewId, userId))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("updateLike")
    class UpdateLike {

        @Test
        @DisplayName("Translates duplicate vote persistence race to BadRequestException")
        void updateLike_duplicateVoteRace_throwsBadRequestException() {
            UUID productId = UUID.randomUUID();
            UUID reviewId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            when(productReviewLikeRepository.findByUserIdAndProductReviewId(userId, reviewId))
                    .thenReturn(java.util.Optional.empty());
            when(productReviewLikeRepository.saveAndFlush(any()))
                    .thenThrow(new DataIntegrityViolationException("duplicate"));

            assertThatThrownBy(() -> service.updateLike(productId, reviewId, userId, true))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("changed concurrently");
        }
    }
}
