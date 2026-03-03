package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.review.converter.ProductReviewDtoConverter;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.entity.ProductReviewLike;
import com.zufar.icedlatte.review.repository.ProductReviewLikeRepository;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.validator.ProductReviewValidator;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewLikesUpdater {

    private final ProductReviewLikeRepository productReviewLikeRepository;
    private final ProductReviewRepository productReviewRepository;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final ProductReviewDtoConverter productReviewDtoConverter;
    private final ProductReviewValidator productReviewValidator;
    private final ProductReviewProvider productReviewProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ProductReviewDto update(final UUID productId,
                                   final UUID productReviewId,
                                   final Boolean newProductReviewLike) {
        var userId = securityPrincipalProvider.getUserId();

        productReviewValidator.validateProductIdIsValid(productId, productReviewId);

        Optional<ProductReviewLike> productReviewLike = productReviewLikeRepository.findByUserIdAndProductReviewId(userId, productReviewId);

        productReviewLike.ifPresentOrElse(
                productReviewLikeEntity -> {
                    if (productReviewLikeEntity.getIsLike().equals(newProductReviewLike)) {
                        productReviewLikeRepository.deleteByProductIdAndProductReviewId(productId, productReviewId);
                    } else {
                        productReviewLikeEntity.setIsLike(newProductReviewLike);
                        productReviewLikeRepository.saveAndFlush(productReviewLikeEntity);
                    }
                },
                () -> {
                    ProductReviewLike newReviewLike = ProductReviewLike.builder()
                            .userId(userId)
                            .productId(productId)
                            .productReviewId(productReviewId)
                            .isLike(newProductReviewLike)
                            .build();
                    productReviewLikeRepository.saveAndFlush(newReviewLike);
                }
        );

        productReviewRepository.updateLikesCount(productReviewId);
        productReviewRepository.updateDislikesCount(productReviewId);

        ProductReview productReview = productReviewProvider.getReviewEntityById(productReviewId);
        return productReviewDtoConverter.toProductReviewDto(productReview);
    }
}
