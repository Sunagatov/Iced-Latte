package com.zufar.icedlatte.review.repository;

import com.zufar.icedlatte.review.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {

    Optional<ProductReview> findByUserIdAndProductId(UUID userId, UUID productId);

    Page<ProductReview> findAllByProductId(UUID productId, Pageable pageable);
}
