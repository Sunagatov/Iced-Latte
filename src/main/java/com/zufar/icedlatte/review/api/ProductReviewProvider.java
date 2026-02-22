package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.exception.ProductReviewNotFoundException;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductReviewProvider {

    private final ProductReviewRepository reviewRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ProductReview getReviewEntityById(final UUID productReviewId) {
        return reviewRepository.findById(productReviewId)
                .orElseThrow(() -> new ProductReviewNotFoundException(productReviewId));
    }
}
