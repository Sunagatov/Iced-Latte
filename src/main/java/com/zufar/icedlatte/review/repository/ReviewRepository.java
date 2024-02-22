package com.zufar.icedlatte.review.repository;

import com.zufar.icedlatte.review.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<ProductReview, UUID> {
}
