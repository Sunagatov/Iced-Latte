package com.zufar.icedlatte.review.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

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

    // TODO: This field is dead, but removing it requires a DB migration for a harmless column.
    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;
}
