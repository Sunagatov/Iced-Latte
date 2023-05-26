package com.zufar.onlinestore.review.repository;

import com.zufar.onlinestore.review.entity.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {

    List<Review> findByProductId(String productId);
}

