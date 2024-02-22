package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.openapi.dto.ProductReviewsAndRatingsWithPagination;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class PageableReviewsProvider {
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ProductReviewsAndRatingsWithPagination getProductReviews(final UUID productId, final Integer page,
                                                                    final Integer size,
                                                                    final String sortAttribute,
                                                                    final String sortDirection) {
        Pageable pageable = createPageableObject(page, size, sortAttribute, sortDirection);

        throw new UnsupportedOperationException("getProductReviews has not been implemenetd");
    }

}