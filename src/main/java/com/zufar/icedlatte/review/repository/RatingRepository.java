package com.zufar.icedlatte.review.repository;

import com.zufar.icedlatte.review.entity.ProductRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<ProductRating, UUID> {
}
