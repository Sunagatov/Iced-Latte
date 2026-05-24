package com.zufar.icedlatte.review.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zufar.icedlatte.review.entity.ProductReviewLike;

public interface ProductReviewLikeRepository extends JpaRepository<ProductReviewLike, UUID> {

    Optional<ProductReviewLike> findByUserIdAndProductReviewId(UUID userId, UUID reviewId);

    void deleteByUserIdAndProductReviewId(UUID userId, UUID productReviewId);
}
