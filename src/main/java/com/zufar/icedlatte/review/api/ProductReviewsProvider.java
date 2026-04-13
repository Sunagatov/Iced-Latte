package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.common.config.PaginationConfig;
import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.openapi.dto.ProductReviewsAndRatingsWithPagination;
import com.zufar.icedlatte.review.converter.ProductReviewDtoConverter;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.validator.GetReviewsRequestValidator;
import com.zufar.icedlatte.review.validator.ProductReviewValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import com.zufar.icedlatte.common.pagination.PageRequestFactory;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewsProvider {

    private final ProductReviewRepository reviewRepository;
    private final ProductReviewDtoConverter productReviewDtoConverter;
    private final ProductReviewValidator productReviewValidator;
    private final PaginationConfig paginationConfig;
    private final GetReviewsRequestValidator getReviewsRequestValidator;

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
        String sortAttr = sortAttribute != null ? sortAttribute : paginationConfig.getReviews().getDefaultSortAttribute();
        String sortDir = sortDirection != null ? sortDirection : paginationConfig.getReviews().getDefaultSortDirection();

        var responsePage = reviewRepository
                .findAllProductReviews(productId, productRatings, PageRequestFactory.of(page, size, sortAttr, sortDir))
                .map(productReviewDtoConverter::toProductReviewDto);
        return productReviewDtoConverter.toProductReviewsAndRatingsWithPagination(responsePage);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ProductReviewDto getProductReviewForUser(final UUID productId, final UUID userId) {
        productReviewValidator.validateProductExists(productId);
        return reviewRepository.findByUserIdAndProductId(userId, productId)
                .map(productReviewDtoConverter::toProductReviewDto)
                .orElseThrow(() -> new com.zufar.icedlatte.review.exception.ProductReviewNotFoundException(productId));
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ProductReviewsAndRatingsWithPagination getUserReviews(final UUID userId,
                                                                 final Integer pageNumber,
                                                                 final Integer pageSize,
                                                                 final String sortAttribute,
                                                                 final String sortDirection) {
        int page = pageNumber != null ? pageNumber : paginationConfig.getDefaultPageNumber();
        int size = pageSize != null ? pageSize : paginationConfig.getReviews().getDefaultPageSize();
        String sortAttr = sortAttribute != null ? sortAttribute : paginationConfig.getReviews().getDefaultSortAttribute();
        String sortDir = sortDirection != null ? sortDirection : paginationConfig.getReviews().getDefaultSortDirection();
        getReviewsRequestValidator.validate(page, size, sortAttr, sortDir, null);
        var responsePage = reviewRepository
                .findAllByUserId(userId, PageRequestFactory.of(page, size, sortAttr, sortDir))
                .map(productReviewDtoConverter::toProductReviewDto);
        return productReviewDtoConverter.toProductReviewsAndRatingsWithPagination(responsePage);
    }
}
