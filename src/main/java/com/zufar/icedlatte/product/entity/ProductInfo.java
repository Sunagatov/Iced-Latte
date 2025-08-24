package com.zufar.icedlatte.product.entity;

import com.zufar.icedlatte.common.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "product",
        indexes = {
                @Index(name = "idx_product_price", columnList = "price"),
                @Index(name = "idx_product_brand", columnList = "brand_name"),
                @Index(name = "idx_product_seller", columnList = "seller_name"),
                @Index(name = "idx_product_avg_rating", columnList = "average_rating"),
                @Index(name = "idx_product_popularity", columnList = "popularity_score")
        }
)
public class ProductInfo extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID productId;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", nullable = false, length = 4000)
    private String description;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "average_rating", precision = 2, scale = 1)
    private BigDecimal averageRating;

    @Column(name = "reviews_count")
    private Integer reviewsCount;

    @Column(name = "brand_name", nullable = false, length = 255)
    private String brandName;

    @Column(name = "seller_name", nullable = false, length = 255)
    private String sellerName;

    @Column(name = "origin_country", nullable = false, length = 128)
    private String originCountry;

    @Column(name = "weight", nullable = false)
    private int weight;

    @Column(name = "size_length", nullable = false)
    private int lengthSize;

    @Column(name = "size_width", nullable = false)
    private int widthSize;

    @Column(name = "size_height", nullable = false)
    private int heightSize;

    @Column(name = "sold_products_count", nullable = false)
    private int soldProductsCount;

    @Column(name = "discount", nullable = false)
    private int discount;

    @CreationTimestamp
    @Column(name = "date_added", nullable = false, updatable = false)
    private LocalDateTime dateAdded;

    @Column(name = "popularity_score", nullable = false)
    private int popularityScore;

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ProductInfo productInfo)) {
            return false;
        }
        return new EqualsBuilder()
                .append(productId, productInfo.productId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(productId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("productId", productId)
                .toString();
    }
}