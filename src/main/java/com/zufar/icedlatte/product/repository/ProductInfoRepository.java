package com.zufar.icedlatte.product.repository;

import com.zufar.icedlatte.product.entity.ProductInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductInfoRepository extends JpaRepository<ProductInfo, UUID>, JpaSpecificationExecutor<ProductInfo> {

    @Query("SELECT DISTINCT p.sellerName FROM ProductInfo p ORDER BY p.sellerName")
    List<String> findDistinctSellerNames();

    @Query("SELECT DISTINCT p.brandName FROM ProductInfo p ORDER BY p.brandName")
    List<String> findDistinctBrandNames();

    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE product p
               SET average_rating = COALESCE((SELECT AVG(pr.rating)
                                               FROM product_reviews pr
                                              WHERE pr.product_id = p.id), 0)
             WHERE p.id = :productId
            """)
    void updateAverageRating(@Param("productId") UUID productId);

    @Modifying
    @Query(nativeQuery = true, value = """
            UPDATE product p
               SET reviews_count = (SELECT COUNT(pr.id)
                                      FROM product_reviews pr
                                     WHERE pr.product_id = p.id)
             WHERE p.id = :productId
            """)
    void updateReviewsCount(@Param("productId") UUID productId);

    @SuppressWarnings("SqlWithoutWhereClause")
    @Modifying
    @Query(nativeQuery = true, value = """
            -- noinspection SqlWithoutWhere
UPDATE product p
               SET average_rating = COALESCE((SELECT AVG(pr.rating)
                                               FROM product_reviews pr
                                              WHERE pr.product_id = p.id), 0)
            """)
    void updateAllAverageRatings();

    @SuppressWarnings("SqlWithoutWhereClause")
    @Modifying
    @Query(nativeQuery = true, value = """
            -- noinspection SqlWithoutWhere
UPDATE product p
               SET reviews_count = (SELECT COUNT(pr.id)
                                      FROM product_reviews pr
                                     WHERE pr.product_id = p.id)
            """)
    void updateAllReviewsCounts();
}
