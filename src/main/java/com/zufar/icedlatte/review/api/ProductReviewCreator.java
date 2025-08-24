package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.openapi.dto.ProductReviewRequest;
import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.review.converter.ProductReviewDtoConverter;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.validator.ProductReviewValidator;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewCreator {

    private final ProductReviewRepository reviewRepository;
    private final ProductReviewDtoConverter productReviewDtoConverter;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final SingleUserProvider singleUserProvider;
    private final ProductReviewValidator productReviewValidator;
    private final ProductInfoRepository productInfoRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ProductReviewDto create(final UUID productId,
                                   final ProductReviewRequest productReviewRequest) {
        var userId = securityPrincipalProvider.getUserId();
        var productReviewText = productReviewRequest.getText();

        productReviewValidator.validateProductExists(productId);
        productReviewValidator.validateReviewText(productReviewText);
        productReviewValidator.validateReviewExistsForUser(userId, productId);

        var productReview = ProductReview.builder()
                .user(singleUserProvider.getUserEntityById(userId))
                .productId(productId)
                .text(productReviewText.trim())
                .productRating(productReviewRequest.getRating())
                .likesCount(0)
                .dislikesCount(0)
                .build();

        reviewRepository.saveAndFlush(productReview);

        productInfoRepository.updateAverageRating(productId);
        productInfoRepository.updateReviewsCount(productId);

        return productReviewDtoConverter.toProductReviewDto(productReview);
    }
}
