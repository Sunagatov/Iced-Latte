package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.openapi.dto.ProductReviewRequest;
import com.zufar.icedlatte.openapi.dto.ProductReviewResponse;
import com.zufar.icedlatte.product.api.SingleProductProvider;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.review.converter.ProductReviewDtoConverter;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.entity.UserEntity;
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
    private final SingleProductProvider singleProductProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ProductReviewResponse create(final UUID productId, final ProductReviewRequest productReviewRequest) {
        var userId = securityPrincipalProvider.getUserId();
        var text = productReviewRequest.getText().trim();
        final UserEntity user = singleUserProvider.getUserEntityById(userId);
        final ProductInfo product = singleProductProvider.getProductEntityById(productId);
        productReviewValidator.validateReview(userId, productId, text);

        var review = ProductReview.builder()
                .user(user)
                .productInfo(product)
                .text(text)
                .build();
        reviewRepository.saveAndFlush(review);

        return productReviewDtoConverter.toReviewResponse(review);
    }
}
