package com.zufar.onlinestore.review.repository;

import com.zufar.onlinestore.review.entity.Review;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
public class ReviewMongoDatabaseInitializer {

    @Autowired
    private ReviewRepository reviewRepository;

    @Bean
    CommandLineRunner reviewCommandLineRunner() {
        return strings -> {
            int count = new Random().nextInt(3);
            createReviewsForProduct("1", count);
            createReviewsForProduct("2", count);
            createReviewsForProduct("3", count);
            createReviewsForProduct("4", count);
        };
    }

    private void createReviewsForProduct(String productId, int count) {
        for (int i = 1; i <= count; i++) {
            Review review = new Review();
            review.setProductId(productId);
            review.setCustomerId("Customer" + i);
            review.setText("Review " + i + " for Product " + productId);
            review.setRating(new Random().nextInt(1,5));
            reviewRepository.save(review);
        }
    }
}
