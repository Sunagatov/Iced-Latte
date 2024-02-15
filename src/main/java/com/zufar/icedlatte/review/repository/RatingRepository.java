package com.zufar.icedlatte.review.repository;

import com.zufar.icedlatte.review.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {

    @Query("SELECT pr.mark, COUNT(pr) FROM Rating pr WHERE pr.productInfo.id = :productId GROUP BY pr.mark")
    List<Object[]> countRatingMarksByProductId(UUID productId);
}
