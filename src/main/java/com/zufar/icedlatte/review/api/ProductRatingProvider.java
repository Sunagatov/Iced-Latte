package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.openapi.dto.AverageProductRatingDto;
import com.zufar.icedlatte.review.converter.ProductRatingDtoConverter;
import com.zufar.icedlatte.review.repository.ProductRatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductRatingProvider {

    private final ProductRatingRepository ratingRepository;
    private final ProductRatingDtoConverter ratingConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public AverageProductRatingDto getAvgRatingByProductId(final UUID productId) {
        Double averageRating = ratingRepository.getAvgRatingByProductId(productId);
        return new AverageProductRatingDto(productId, averageRating);
    }
}
