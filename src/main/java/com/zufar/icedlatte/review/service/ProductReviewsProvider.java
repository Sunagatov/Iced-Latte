package com.zufar.icedlatte.review.service;

import com.zufar.icedlatte.common.config.PaginationConfig;
import com.zufar.icedlatte.common.exception.NotFoundException;
import com.zufar.icedlatte.common.pagination.PageRequestFactory;
import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.openapi.dto.ProductReviewRatingStats;
import com.zufar.icedlatte.openapi.dto.ProductReviewsAndRatingsWithPagination;
import com.zufar.icedlatte.openapi.dto.RatingMap;
import com.zufar.icedlatte.review.converter.ProductReviewDtoConverter;
import com.zufar.icedlatte.review.dto.ProductRatingCount;
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
        var pageRequest = buildValidatedReviewsPageRequest(pageNumber, pageSize, sortAttribute, sortDirection, productRatings);

        var responsePage = reviewRepository
                .findAllProductReviews(productId, productRatings, pageRequest)
                .map(productReviewDtoConverter::toProductReviewDto);
        return productReviewDtoConverter.toProductReviewsAndRatingsWithPagination(responsePage);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ProductReviewDto getProductReviewForUser(final UUID productId, final UUID userId) {
        productReviewValidator.validateProductExists(productId);
        return reviewRepository.findByUserIdAndProductId(userId, productId)
                .map(productReviewDtoConverter::toProductReviewDto)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Product's review for productId = '%s' and userId = '%s' was not found", productId, userId)));
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ProductReviewsAndRatingsWithPagination getUserReviews(final UUID userId,
                                                                 final Integer pageNumber,
                                                                 final Integer pageSize,
                                                                 final String sortAttribute,
                                                                 final String sortDirection) {
        var responsePage = reviewRepository
                .findAllByUserId(userId, buildValidatedReviewsPageRequest(pageNumber, pageSize, sortAttribute, sortDirection, null))
                .map(productReviewDtoConverter::toProductReviewDto);
        return productReviewDtoConverter.toProductReviewsAndRatingsWithPagination(responsePage);
    }

    private org.springframework.data.domain.Pageable buildValidatedReviewsPageRequest(Integer pageNumber,
                                                                                      Integer pageSize,
                                                                                      String sortAttribute,
                                                                                      String sortDirection,
                                                                                      List<Integer> productRatings) {
        int page = pageNumber != null ? pageNumber : paginationConfig.defaultPageNumber();
        int size = pageSize != null ? pageSize : paginationConfig.reviews().defaultPageSize();
        String sortAttr = sortAttribute != null ? sortAttribute : paginationConfig.reviews().defaultSortAttribute();
        String sortDir = sortDirection != null ? sortDirection : paginationConfig.reviews().defaultSortDirection();
        getReviewsRequestValidator.validate(page, size, sortAttr, sortDir, productRatings);
        return PageRequestFactory.of(page, size, sortAttr, sortDir);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ProductReviewRatingStats getStatistics(final UUID productId) {
        productReviewValidator.validateProductExists(productId);

        Double avg = reviewRepository.getAvgRatingByProductId(productId);
        double avgRating = avg != null ? avg : 0.0;
        return new ProductReviewRatingStats(productId,
                Math.round(avgRating * 10.0) / 10.0,
                reviewRepository.getReviewCountProductById(productId),
                getProductRatingMap(productId));
    }

    private RatingMap getProductRatingMap(UUID productId) {
        List<ProductRatingCount> productRatingCountPairs = reviewRepository.getRatingsMapByProductId(productId);
        if (productRatingCountPairs == null) {
            return new RatingMap();
        }
        return productReviewDtoConverter.convertToProductRatingMap(productRatingCountPairs);
    }
}
