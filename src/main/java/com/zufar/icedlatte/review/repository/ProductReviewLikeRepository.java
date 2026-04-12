package com.zufar.icedlatte.review.repository;

import com.zufar.icedlatte.review.entity.ProductReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductReviewLikeRepository extends JpaRepository<ProductReviewLike, UUID> {

    Optional<ProductReviewLike> findByUserIdAndProductReviewId(UUID userId, UUID reviewId);

    void deleteByUserIdAndProductReviewId(UUID userId, UUID productReviewId);

}
