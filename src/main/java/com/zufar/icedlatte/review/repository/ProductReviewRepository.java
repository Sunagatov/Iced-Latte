package com.zufar.icedlatte.review.repository;

import com.zufar.icedlatte.review.dto.ProductRatingCount;
import com.zufar.icedlatte.review.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {

    Optional<ProductReview> findByUserIdAndProductId(UUID userId, UUID productId);

    @Query("SELECT review FROM ProductReview review " +
            "WHERE review.productId = :productId AND " +
            "(:productRatings IS NULL OR review.productRating IN :productRatings) ")
    Page<ProductReview> findAllProductReviews(@Param("productId") UUID productId,
                                              @Param("productRatings") List<Integer> productRatings,
                                              Pageable pageable);

    @Query("SELECT COUNT(pr) " +
            "FROM ProductReview pr " +
            "WHERE pr.productId = :productId")
    Integer getReviewCountProductById(UUID productId);

    @Query("SELECT AVG(pr.productRating) " +
            "FROM ProductReview pr " +
            "WHERE pr.productId = :productId")
    Double getAvgRatingByProductId(UUID productId);

    @Query("SELECT new com.zufar.icedlatte.review.dto.ProductRatingCount(productReview.productRating, COUNT(productReview.productRating)) " +
            "FROM ProductReview productReview " +
            "WHERE productReview.productId = :productId " +
            "GROUP BY productReview.productRating")
    List<ProductRatingCount> getRatingsMapByProductId(UUID productId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true,
            value = "UPDATE product_reviews " +
                    "SET likes_count = (" +
                        "SELECT count(product_reviews_likes.id) " +
                        "FROM product_reviews_likes " +
                        "WHERE product_reviews_likes.is_like = true AND product_reviews_likes.review_id = :productReviewId" +
                    ") " +
                    "WHERE product_reviews.id = :productReviewId")
    void updateLikesCount(final UUID productReviewId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true,
            value = "UPDATE product_reviews " +
                    "SET dislikes_count = (" +
                        "SELECT count(product_reviews_likes.id) " +
                        "FROM product_reviews_likes " +
                        "WHERE product_reviews_likes.is_like = false AND product_reviews_likes.review_id = :productReviewId" +
                    ") " +
                    "WHERE product_reviews.id = :productReviewId")
    void updateDislikesCount(final UUID productReviewId);

    @SuppressWarnings("SqlWithoutWhereClause")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true,
            value = "UPDATE product_reviews pr " +
                    "SET likes_count = (" +
                        "SELECT count(prl.id) " +
                        "FROM product_reviews_likes prl " +
                        "WHERE prl.is_like = true AND prl.review_id = pr.id" +
                    ")")
    void updateAllLikesCounts();

    @SuppressWarnings("SqlWithoutWhereClause")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(nativeQuery = true,
            value = "UPDATE product_reviews pr " +
                    "SET dislikes_count = (" +
                        "SELECT count(prl.id) " +
                        "FROM product_reviews_likes prl " +
                        "WHERE prl.is_like = false AND prl.review_id = pr.id" +
                    ")")
    void updateAllDislikesCounts();
}
