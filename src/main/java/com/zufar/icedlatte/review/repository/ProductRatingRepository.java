package com.zufar.icedlatte.review.repository;

import com.zufar.icedlatte.review.entity.ProductRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRatingRepository extends JpaRepository<ProductRating, UUID> {

    @Query("SELECT AVG(pr.productRating) FROM ProductRating pr WHERE pr.productInfo.id = :productId")
    Double getAvgRatingByProductId(UUID productId);
}
