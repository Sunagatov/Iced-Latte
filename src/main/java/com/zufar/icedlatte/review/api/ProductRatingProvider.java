package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.openapi.dto.ProductReviewRatingStats;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import com.zufar.icedlatte.review.converter.ProductRatingDtoConverter;
import com.zufar.icedlatte.review.repository.ProductRatingRepository;
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
public class ProductRatingProvider {

    private final ProductRatingRepository ratingRepository;
    private final ProductReviewProvider reviewProvider;
    private final ProductRatingDtoConverter ratingConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Double getAvgRatingByProductId(final UUID productId) {
        return ratingRepository.getAvgRatingByProductId(productId);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ProductReviewRatingStats getRatingAndReviewStat(final UUID productId) {
        List<Object[]> listOfMappings = ratingRepository.getRatingsMapByProductId(productId);
        Double avgRating = ratingRepository.getAvgRatingByProductId(productId);
        if (listOfMappings == null || avgRating == null) {
            log.error("The product with id = {} was not found.", productId);
            throw new ProductNotFoundException(productId);
        }
        Integer reviewCount = reviewProvider.getReviewCountProductById(productId);
        return new ProductReviewRatingStats(productId, avgRating, reviewCount,
                ratingConverter.convertToRatingMap(listOfMappings));
    }
}
