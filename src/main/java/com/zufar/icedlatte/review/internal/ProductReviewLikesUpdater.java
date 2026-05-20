package com.zufar.icedlatte.review.internal;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.exception.NotFoundException;
import com.zufar.icedlatte.openapi.dto.ProductReviewDto;
import com.zufar.icedlatte.review.api.ReviewMaintenanceApi;
import com.zufar.icedlatte.review.converter.ProductReviewDtoConverter;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.entity.ProductReviewLike;
import com.zufar.icedlatte.review.repository.ProductReviewLikeRepository;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.validator.ProductReviewValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductReviewLikesUpdater implements ReviewMaintenanceApi {

    private final ProductReviewLikeRepository productReviewLikeRepository;
    private final ProductReviewRepository productReviewRepository;
    private final ProductReviewDtoConverter productReviewDtoConverter;
    private final ProductReviewValidator productReviewValidator;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ProductReviewDto update(final UUID productId,
                                   final UUID productReviewId,
                                   final UUID userId,
                                   final Boolean newProductReviewLike) {
        validateVote(newProductReviewLike);
        productReviewValidator.validateProductIdIsValid(productId, productReviewId);

        Optional<ProductReviewLike> productReviewLike = productReviewLikeRepository.findByUserIdAndProductReviewId(userId, productReviewId);

        productReviewLike.ifPresentOrElse(
                productReviewLikeEntity -> {
                    if (productReviewLikeEntity.getIsLike().equals(newProductReviewLike)) {
                        productReviewLikeRepository.deleteByUserIdAndProductReviewId(userId, productReviewId);
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

        ProductReview productReview = productReviewRepository.findById(productReviewId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Product's review with productReviewId = '%s' was not found", productReviewId)));
        return productReviewDtoConverter.toProductReviewDto(productReview);
    }

    private static void validateVote(Boolean vote) {
        if (vote == null) {
            throw new BadRequestException(
                    "GetReviewsRequest parameters are incorrect. Error messages are [ Review vote 'isLike' must be provided. ].");
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void refreshAllCounts() {
        productReviewRepository.updateAllLikesCounts();
        productReviewRepository.updateAllDislikesCounts();
    }
}
