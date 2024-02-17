package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.openapi.dto.AddNewMarkToProductRequest;
import com.zufar.icedlatte.openapi.dto.RatingDto;
import com.zufar.icedlatte.product.api.ProductApi;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.review.converter.RatingConverter;
import com.zufar.icedlatte.review.entity.Rating;
import com.zufar.icedlatte.review.repository.RatingRepository;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.entity.UserEntity;
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
    public RatingDto addRating(final AddNewMarkToProductRequest addNewMarkToProductRequest, final UUID userId) {
        final UserEntity user = singleUserProvider.getUserEntityById(userId);
        final ProductInfo product = productApi.getProductEntityById(addNewMarkToProductRequest.getProductId());

        Rating rating = Rating.builder()
                .user(user)
                .productInfo(product)
                .mark(addNewMarkToProductRequest.getMark())
                .build();

        Rating savedRating = ratingRepository.save(rating);
        return ratingConverter.convertToDto(savedRating);
    }
}
