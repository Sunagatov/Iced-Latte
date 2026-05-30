package com.zufar.icedlatte.product.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;

import com.zufar.icedlatte.common.audit.AuditableEntity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "product",
        indexes = {
            @Index(name = "idx_product_price", columnList = "price"),
            @Index(name = "idx_product_brand", columnList = "brand_name"),
            @Index(name = "idx_product_seller", columnList = "seller_name"),
            @Index(name = "idx_product_avg_rating", columnList = "average_rating"),
            @Index(name = "idx_product_popularity", columnList = "popularity_score")
        })
public class ProductInfo extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    @ToString.Include
    private UUID id;

    @Version
    @Column(name = "version")
    private long version;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "description", nullable = false, length = 4000)
    private String description;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "average_rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal averageRating;

    @Column(name = "reviews_count", nullable = false)
    private Integer reviewsCount;

    @Column(name = "brand_name", nullable = false, length = 64)
    private String brandName;

    @Column(name = "seller_name", nullable = false, length = 64)
    private String sellerName;

    @Column(name = "origin_country", nullable = false, length = 128)
    private String originCountry;

    @Column(name = "weight", nullable = false)
    private int weight;

    @Column(name = "size_length", nullable = false)
    private int length;

    @Column(name = "size_width", nullable = false)
    private int width;

    @Column(name = "size_height", nullable = false)
    private int height;

    @Column(name = "sold_products_count", nullable = false)
    private int soldProductsCount;

    @Column(name = "discount", nullable = false)
    private int discount;

    @CreationTimestamp
    @Column(name = "date_added", nullable = false, updatable = false)
    private LocalDateTime dateAdded;

    @Column(name = "popularity_score", nullable = false)
    private int popularityScore;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ProductInfo productInfo)) return false;
        if (id == null || productInfo.id == null) return false;
        return Objects.equals(id, productInfo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
