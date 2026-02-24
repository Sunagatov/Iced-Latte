package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.openapi.dto.ProductReviewRequest;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.review.converter.ProductReviewDtoConverter;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.exception.EmptyProductReviewException;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.validator.ProductReviewValidator;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.entity.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReviewCreator unit tests")
class ProductReviewCreatorTest {

    @Mock
    private ProductReviewRepository reviewRepository;
    @Mock
    private ProductReviewDtoConverter productReviewDtoConverter;
    @Mock
    private SecurityPrincipalProvider securityPrincipalProvider;
    @Mock
    private SingleUserProvider singleUserProvider;
    @Mock
    private ProductReviewValidator productReviewValidator;
    @Mock
    private ProductInfoRepository productInfoRepository;
    @InjectMocks
    private ProductReviewCreator creator;

    @Test
    @DisplayName("Creates review, trims text, updates stats, returns DTO")
    void create_validRequest_savesAndReturnsDto() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).build();
        ProductReviewRequest request = new ProductReviewRequest();
        request.setText("  Great coffee!  ");
        request.setRating(5);
        ProductReview saved = ProductReview.builder().id(UUID.randomUUID()).build();
        ProductReviewDto expectedDto = new ProductReviewDto();

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(singleUserProvider.getUserEntityById(userId)).thenReturn(user);
        when(reviewRepository.saveAndFlush(any(ProductReview.class))).thenReturn(saved);
        when(productReviewDtoConverter.toProductReviewDto(any())).thenReturn(expectedDto);

        ProductReviewDto result = creator.create(productId, request);

        assertThat(result).isEqualTo(expectedDto);

        ArgumentCaptor<ProductReview> captor = ArgumentCaptor.forClass(ProductReview.class);
        verify(reviewRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getText()).isEqualTo("Great coffee!");
        assertThat(captor.getValue().getProductRating()).isEqualTo(5);
        assertThat(captor.getValue().getLikesCount()).isZero();
        assertThat(captor.getValue().getDislikesCount()).isZero();

        verify(productInfoRepository).updateAverageRating(productId);
        verify(productInfoRepository).updateReviewsCount(productId);
    }

    @Test
    @DisplayName("Propagates EmptyProductReviewException from validator")
    void create_emptyText_throwsEmptyProductReviewException() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ProductReviewRequest request = new ProductReviewRequest();
        request.setText("   ");
        request.setRating(3);

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        doThrow(new EmptyProductReviewException()).when(productReviewValidator).validateReviewText("   ");

        assertThatThrownBy(() -> creator.create(productId, request))
                .isInstanceOf(EmptyProductReviewException.class);
    }
}
