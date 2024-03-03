package com.zufar.icedlatte.review.repository;

import com.zufar.icedlatte.review.entity.ProductRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<ProductRating, UUID> {

    @Query("SELECT pr.ratingValue, COUNT(pr) FROM ProductRating pr WHERE pr.productInfo.id = :productId GROUP BY pr.ratingValue")
    List<Object[]> countRatingMarksByProductId(UUID productId);
}
