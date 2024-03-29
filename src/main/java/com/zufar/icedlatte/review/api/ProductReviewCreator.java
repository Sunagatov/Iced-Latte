package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.openapi.dto.ProductReviewRequest;
import com.zufar.icedlatte.openapi.dto.ProductReviewResponse;
import com.zufar.icedlatte.product.api.SingleProductProvider;
import com.zufar.icedlatte.review.converter.ProductReviewDtoConverter;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
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
    private final ProductRatingAndReviewValidator productReviewValidator;
    private final SingleProductProvider singleProductProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ProductReviewResponse create(final UUID productId, final ProductReviewRequest productReviewRequest) {
        var userId = securityPrincipalProvider.getUserId();
        var productReviewText = productReviewRequest.getText().trim();
        productReviewValidator.validateReview(userId, productId, productReviewText);

        var productReview = ProductReview.builder()
                .user(singleUserProvider.getUserEntityById(userId))
                .productInfo(singleProductProvider.getProductEntityById(productId))
                .text(productReviewText)
                .build();

        reviewRepository.saveAndFlush(productReview);

        return productReviewDtoConverter.toReviewResponse(productReview);
    }
}
