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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof final Review other)) return false;
        if (!other.canEqual(this)) return false;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (!Objects.equals(this$id, other$id)) return false;
        final Object this$productId = this.getProductId();
        final Object other$productId = other.getProductId();
        if (!Objects.equals(this$productId, other$productId)) return false;
        final Object this$customerId = this.getCustomerId();
        final Object other$customerId = other.getCustomerId();
        if (!Objects.equals(this$customerId, other$customerId))
            return false;
        final Object this$text = this.getText();
        final Object other$text = other.getText();
        if (!Objects.equals(this$text, other$text)) return false;
        if (this.getRating() != other.getRating()) return false;
        final Object this$date = this.getDate();
        final Object other$date = other.getDate();
        return Objects.equals(this$date, other$date);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof Review;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $productId = this.getProductId();
        result = result * PRIME + ($productId == null ? 43 : $productId.hashCode());
        final Object $customerId = this.getCustomerId();
        result = result * PRIME + ($customerId == null ? 43 : $customerId.hashCode());
        final Object $text = this.getText();
        result = result * PRIME + ($text == null ? 43 : $text.hashCode());
        result = result * PRIME + this.getRating();
        final Object $date = this.getDate();
        result = result * PRIME + ($date == null ? 43 : $date.hashCode());
        return result;
    }

    public String toString() {
        return "Review(id=" +
                this.getId() + ", productId=" +
                this.getProductId() + ", customerId=" +
                this.getCustomerId() + ", text=" +
                this.getText() + ", rating=" +
                this.getRating() + ", date=" +
                this.getDate() + ")";
    }
}