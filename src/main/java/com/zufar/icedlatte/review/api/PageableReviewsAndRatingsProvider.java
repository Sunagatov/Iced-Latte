package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.openapi.dto.ProductReviewsAndRatingsWithPagination;
import com.zufar.icedlatte.review.converter.ProductReviewDtoConverter;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.entity.Rating;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.zufar.icedlatte.common.util.Utils.createPageableObject;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageableReviewsAndRatingsProvider {

    private final ProductReviewRepository reviewRepository;
    private final RatingRepository ratingRepository;
    private final ProductReviewDtoConverter productReviewDtoConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ProductReviewsAndRatingsWithPagination getProductReviews(final UUID productId,
                                                                    final Integer page,
                                                                    final Integer size,
                                                                    final String sortAttribute,
                                                                    final String sortDirection) {
        Pageable pageable = createPageableObject(page, size, sortAttribute, sortDirection);
        Page<ProductReview> reviewsWithPageInfo = reviewRepository.findAllByProductId(productId, pageable);
        Page<Rating> ratingsWithPageInfo = ratingRepository.findAllByProductId(productId, pageable);
        var result = new ProductReviewsAndRatingsWithPagination();
        result.setPage(reviewsWithPageInfo.getTotalPages());
        result.setSize(reviewsWithPageInfo.getSize());
        result.setTotalElements(reviewsWithPageInfo.getTotalElements());
        result.setTotalPages(reviewsWithPageInfo.getTotalPages());
        // TODO: how to merge that?
        return null;
    }

}