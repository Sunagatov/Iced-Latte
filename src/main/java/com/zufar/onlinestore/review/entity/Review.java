package com.zufar.onlinestore.review.entity;

import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.util.Objects;

@Builder
@Document(collection = "review")
public class Review {

    @Id
    private String id;
    private String productId;
    private String customerId;
    private String text;
    private int rating;

    @CreatedDate
    @Field("date")
    private LocalDate date;

    public Review(String id, String productId, String customerId, String text, int rating, LocalDate date) {
        this.id = id;
        this.productId = productId;
        this.customerId = customerId;
        this.text = text;
        this.rating = rating;
        this.date = date;
    }

    public Review() {
    }

    public String getId() {
        return this.id;
    }

    public String getProductId() {
        return this.productId;
    }

    public String getCustomerId() {
        return this.customerId;
    }

    public String getText() {
        return this.text;
    }

    public int getRating() {
        return this.rating;
    }

    public LocalDate getDate() {
        return this.date;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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
}