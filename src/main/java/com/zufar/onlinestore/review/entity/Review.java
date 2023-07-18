package com.zufar.onlinestore.review.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@Table(name = "review")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "text")
    private String text;

    @Column(name = "rating")
    private int rating;

    @CreatedDate
    @Column(name = "date")
    private LocalDate date;

    public Review(UUID id, String productId, String customerId, String text, int rating, LocalDate date) {
        this.id = id;
        this.productId = productId;
        this.customerId = customerId;
        this.text = text;
        this.rating = rating;
        this.date = date;
    }

    public Review() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Review review = (Review) o;
        return id.equals(review.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Review{" +
                "id=" + id +
                ", productId='" + productId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", text='" + text + '\'' +
                ", rating=" + rating +
                ", date=" + date +
                '}';
    }
}