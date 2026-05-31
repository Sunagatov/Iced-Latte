package com.zufar.icedlatte.review.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.openapi.dto.ProductReviewsAndRatingsWithPagination;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.user.api.dto.UserLookupSnapshot;

public class ProductReviewDtoConverterTest {

    private ProductReviewDtoConverter converter;

    @BeforeEach
    void setup() {
        converter = Mappers.getMapper(ProductReviewDtoConverter.class);
    }

    @Test
    @DisplayName("Convert ProductReview entity to ProductReviewDto")
    void converProductReviewToProductReviewDto() {

        ProductReview expectedProductReview = ProductReview.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .productRating(1)
                .text("")
                .createdAt(OffsetDateTime.now())
                .aiSummary("dead summary")
                .build();
        var user = new UserLookupSnapshot(UUID.randomUUID(), "first", "last", "user@example.com");
        ProductReviewDto actualProductReviewDto = converter.toProductReviewDto(expectedProductReview, user);

        assertThat(actualProductReviewDto.getProductReviewId()).isEqualTo(expectedProductReview.getId());
        assertThat(actualProductReviewDto.getProductId()).isEqualTo(expectedProductReview.getProductId());
        assertThat(actualProductReviewDto.getProductRating()).isEqualTo(expectedProductReview.getProductRating());
        assertThat(actualProductReviewDto.getText()).isEqualTo(expectedProductReview.getText());
        assertThat(actualProductReviewDto.getCreatedAt()).isEqualTo(expectedProductReview.getCreatedAt());
        assertThat(actualProductReviewDto.getUserName())
                .isEqualTo(Optional.of(user).get().firstName());
        assertThat(actualProductReviewDto.getUserLastname())
                .isEqualTo(Optional.of(user).get().lastName());
        assertThat(actualProductReviewDto.getLikesCount()).isEqualTo(expectedProductReview.getLikesCount());
        assertThat(actualProductReviewDto.getDislikesCount()).isEqualTo(expectedProductReview.getDislikesCount());
        assertThat(actualProductReviewDto.getAiSummary()).isNull();
    }

    @Test
    @DisplayName("Convert ProductReviewDto page to ProductReviewsAndRatingsWithPagination")
    void convertToProductReviewsAndRatingsWithPagination() {

        ProductReviewDto productReviewDto = new ProductReviewDto(
                UUID.randomUUID(), UUID.randomUUID(), 1, "", OffsetDateTime.now(), "John", "Doe", 0, 0);

        List<ProductReviewDto> productReviewDtos = Arrays.asList(productReviewDto, productReviewDto);
        Page<ProductReviewDto> page = new PageImpl<>(productReviewDtos, PageRequest.of(0, 5), productReviewDtos.size());

        ProductReviewsAndRatingsWithPagination productReviewsAndRatingsWithPagination =
                converter.toProductReviewsAndRatingsWithPagination(page);

        assertThat(productReviewsAndRatingsWithPagination.getReviewsWithRatings())
                .hasSize(productReviewDtos.size());
        assertThat(productReviewsAndRatingsWithPagination.getPage()).isEqualTo(page.getNumber());
        assertThat(productReviewsAndRatingsWithPagination.getSize()).isEqualTo(page.getSize());
        assertThat(productReviewsAndRatingsWithPagination.getTotalElements()).isEqualTo(page.getTotalElements());
        assertThat(productReviewsAndRatingsWithPagination.getTotalPages()).isEqualTo(page.getTotalPages());
        assertThat(productReviewsAndRatingsWithPagination.getReviewsWithRatings())
                .isEqualTo(page.getContent());
    }
}
