package com.zufar.onlinestore.cart.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shopping_session")
public class ShoppingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL)
    private Collection<ShoppingSessionItem> items;

    @Column(name = "items_quantity", nullable = false)
    private Integer itemsQuantity;

    @Column(name = "products_quantity", nullable = false)
    private Integer productsQuantity;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at", nullable = false)
    private LocalDateTime closedAt;

    public ShoppingSession(UUID id, UUID userId, Collection<ShoppingSessionItem> items, Integer itemsQuantity, Integer productsQuantity, LocalDateTime createdAt, LocalDateTime closedAt) {
        this.id = id;
        this.userId = userId;
        this.items = items;
        this.itemsQuantity = itemsQuantity;
        this.productsQuantity = productsQuantity;
        this.createdAt = createdAt;
        this.closedAt = closedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ShoppingSession that = (ShoppingSession) o;
        return Objects.equals(itemsQuantity, that.itemsQuantity) &&
                Objects.equals(productsQuantity, that.productsQuantity) &&
                Objects.equals(id, that.id) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(closedAt, that.closedAt) &&
                Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, itemsQuantity, productsQuantity, createdAt, closedAt, items);
    }

    @Override
    public String toString() {
        return "ShoppingSession{" +
                "id=" + id +
                ", userId=" + userId +
                ", itemsQuantity=" + itemsQuantity +
                ", productsQuantity=" + productsQuantity +
                ", createdAt=" + createdAt +
                ", closedAt=" + closedAt +
                ", items=" + items +
                '}';
    }
}