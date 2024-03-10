package com.zufar.icedlatte.review.repository;

import com.zufar.icedlatte.openapi.dto.ProductReviewWithRating;
import com.zufar.icedlatte.review.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {

    Optional<ProductReview> findByUserIdAndProductInfoProductId(UUID userId, UUID productId);

    @Query("SELECT new com.zufar.icedlatte.openapi.dto.ProductReviewWithRating(r.productRating, pr.text, COALESCE(pr.createdAt, r.createdAt), u.firstName, u.lastName) " +
            "FROM ProductReview pr " +
            "FULL JOIN ProductRating r ON r.user = pr.user AND pr.productInfo.productId = r.productInfo.productId " +
            "JOIN UserEntity u on u.id=coalesce(r.user.id, pr.user.id) " +
            "WHERE pr.productInfo.productId = :productId OR r.productInfo.productId = :productId")
    Page<ProductReviewWithRating> findByProductIdWithRatings(@Param("productId") UUID productId, Pageable pageable);
}
