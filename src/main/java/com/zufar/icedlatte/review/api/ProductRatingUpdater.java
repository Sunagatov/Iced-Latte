package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.openapi.dto.ProductRatingDto;
import com.zufar.icedlatte.product.api.ProductApi;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.review.converter.ProductRatingDtoConverter;
import com.zufar.icedlatte.review.entity.ProductRating;
import com.zufar.icedlatte.review.repository.ProductRatingRepository;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRatingUpdater {

    private final ProductRatingRepository ratingRepository;
    private final SingleUserProvider singleUserProvider;
    private final ProductApi productApi;
    private final ProductRatingDtoConverter ratingConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ProductRatingDto addRating(final UUID userId, final UUID productId, final Integer rating) {
        final UserEntity user = singleUserProvider.getUserEntityById(userId);
        final ProductInfo product = productApi.getProductEntityById(productId);
        ProductRating productRatingEntity = ratingRepository.findByUserIdAndProductInfoProductId(userId, productId);
        if (productRatingEntity == null) {
            log.info("The first request to set rating");
            productRatingEntity = ProductRating.builder()
                    .user(user)
                    .productInfo(product)
                    .build();
        } else {
            log.info("Updating existing product rating entry: {}", productRatingEntity.getId());
        }
        productRatingEntity.setProductRating(rating);
        ratingRepository.save(productRatingEntity);
        return ratingConverter.convertToDto(productRatingEntity);
    }
}
