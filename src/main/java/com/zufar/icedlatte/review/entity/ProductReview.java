package com.zufar.icedlatte.review.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "product_reviews")
public class ProductReview {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "product_id")
    private UUID productId;

    @CreationTimestamp
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "text", nullable = false, length = 1500)
    private String text;

    @Column(name = "rating", nullable = false)
    private Integer productRating;

    @Column(name = "likes_count", nullable = false)
    private Integer likesCount;

    @Column(name = "dislikes_count", nullable = false)
    private Integer dislikesCount;
}
