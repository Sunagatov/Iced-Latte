package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.openapi.dto.AddNewProductRatingRequest;
import com.zufar.icedlatte.openapi.dto.ProductRatingDto;
import com.zufar.icedlatte.product.api.ProductApi;
import com.zufar.icedlatte.review.converter.RatingConverter;
import com.zufar.icedlatte.review.entity.ProductRating;
import com.zufar.icedlatte.review.repository.RatingRepository;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RatingUpdater {

    private final RatingRepository ratingRepository;
    private final SingleUserProvider singleUserProvider;
    private final ProductApi productApi;
    private final RatingConverter ratingConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ProductRatingDto addRating(final AddNewProductRatingRequest addNewProductRatingRequest, final UUID userId) {
        ProductRating productRating = ProductRating.builder()
                .user(singleUserProvider.getUserEntityById(userId))
                .productInfo(productApi.getProductEntityById(addNewProductRatingRequest.getProductId()))
                .productRating(addNewProductRatingRequest.getProductRating())
                .build();

        ProductRating savedProductRating = ratingRepository.save(productRating);
        return ratingConverter.convertToDto(savedProductRating);
    }
}
