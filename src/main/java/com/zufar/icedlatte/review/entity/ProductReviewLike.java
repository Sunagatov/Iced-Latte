package com.zufar.icedlatte.review.entity;

import java.util.UUID;

import jakarta.persistence.*;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "product_reviews_likes")
public class ProductReviewLike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @Column(name = "review_id", nullable = false)
    private UUID productReviewId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "is_like", nullable = false)
    private Boolean isLike;
}
