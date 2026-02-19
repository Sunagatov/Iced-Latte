package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.common.config.PaginationConfig;
import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.openapi.dto.ProductReviewsAndRatingsWithPagination;
import com.zufar.icedlatte.review.converter.ProductReviewDtoConverter;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.validator.ProductReviewValidator;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.zufar.icedlatte.common.util.Utils.createPageableObject;
import static com.zufar.icedlatte.review.converter.ProductReviewDtoConverter.EMPTY_PRODUCT_REVIEW_RESPONSE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewsProvider {

    private final ProductReviewRepository reviewRepository;
    private final ProductReviewDtoConverter productReviewDtoConverter;
    private final ProductReviewValidator productReviewValidator;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final PaginationConfig paginationConfig;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ProductReviewsAndRatingsWithPagination getProductReviews(final UUID productId,
                                                                    final Integer pageNumber,
                                                                    final Integer pageSize,
                                                                    final String sortAttribute,
                                                                    final String sortDirection,
                                                                    final List<Integer> productRatings) {
        productReviewValidator.validateProductExists(productId);

        int page = pageNumber != null ? pageNumber : paginationConfig.getDefaultPageNumber();
        int size = pageSize != null ? pageSize : paginationConfig.getReviews().getDefaultPageSize();
        if (page < 0) {
            throw new IllegalArgumentException("Page number must be non-negative, got: " + page);
        }
        if (size < 1) {
            throw new IllegalArgumentException("Page size must be at least 1, got: " + size);
        }
        String sortAttr = sortAttribute != null ? sortAttribute : paginationConfig.getReviews().getDefaultSortAttribute();
        String sortDir = sortDirection != null ? sortDirection : paginationConfig.getReviews().getDefaultSortDirection();

        var responsePage = reviewRepository
                .findAllProductReviews(productId, productRatings, createPageableObject(page, size, sortAttr, sortDir))
                .map(productReviewDtoConverter::toProductReviewDto);
        return productReviewDtoConverter.toProductReviewsAndRatingsWithPagination(responsePage);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ProductReviewDto getProductReviewForUser(final UUID productId) {
        productReviewValidator.validateProductExists(productId);
        var userId = securityPrincipalProvider.getUserId();
        return reviewRepository.findByUserIdAndProductId(userId, productId)
                .map(productReviewDtoConverter::toProductReviewDto)
                .orElse(EMPTY_PRODUCT_REVIEW_RESPONSE);
    }
}