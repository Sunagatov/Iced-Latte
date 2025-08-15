package com.zufar.icedlatte.product.repository;

import com.zufar.icedlatte.product.entity.ProductInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductInfoRepository extends JpaRepository<ProductInfo, UUID> {

    @Query("""
           SELECT p FROM ProductInfo p
           WHERE (:minPrice IS NULL OR p.price >= :minPrice)
             AND (:maxPrice IS NULL OR p.price <= :maxPrice)
             AND (:minimumAverageRating IS NULL OR p.averageRating >= :minimumAverageRating)
             AND (:brandNames IS NULL OR p.brandName IN :brandNames)
             AND (:sellerNames IS NULL OR p.sellerName IN :sellerNames)
           """)
    Page<ProductInfo> findAllProducts(@Param("minPrice") BigDecimal minPrice,
                                      @Param("maxPrice") BigDecimal maxPrice,
                                      @Param("minimumAverageRating") BigDecimal minimumAverageRating,
                                      @Param("brandNames") List<String> brandNames,
                                      @Param("sellerNames") List<String> sellerNames,
                                      Pageable pageable);

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
}
