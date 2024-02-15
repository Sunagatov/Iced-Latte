package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.openapi.dto.NewReview;
import com.zufar.icedlatte.openapi.dto.ReviewResponse;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.review.converter.ReviewDtoConverter;
import com.zufar.icedlatte.review.entity.Review;
import com.zufar.icedlatte.review.exception.UnsupportedReviewFormatException;
import com.zufar.icedlatte.review.repository.ReviewRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewCreator {

    private final ReviewRepository reviewRepository;
    private final ProductInfoRepository productInfoRepository;
    private final ReviewDtoConverter reviewDtoConverter;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ReviewResponse create(final UUID productId, final NewReview newReview) {
        UUID userId = securityPrincipalProvider.getUserId();
        var text = newReview.getText().trim();
        if (text.isEmpty()) {
            throw new UnsupportedReviewFormatException();
        }
        var product = productInfoRepository.findById(productId);
        if (product.isEmpty()) {
            log.error("The product with id = {} is not found.", productId);
            throw new ProductNotFoundException(productId);
        }

        var review = Review.builder()
                .userId(userId)
                .productId(productId)
                .createdAt(OffsetDateTime.now())
                .text(text)
                .build();
        reviewRepository.save(review);
        return reviewDtoConverter.toReviewResponse(review);
    }
}
