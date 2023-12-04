package com.zufar.icedlatte.cart.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shopping_cart")
public class ShoppingCart {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToMany(mappedBy = "shoppingCart",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE,
                    CascadeType.REFRESH, CascadeType.REMOVE, CascadeType.DETACH},
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    private Set<ShoppingCartItem> items;

    @Column(name = "items_quantity", nullable = false)
    private Integer itemsQuantity;

    @Column(name = "products_quantity", nullable = false)
    private Integer productsQuantity;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    private static final int DEFAULT_PRODUCTS_QUANTITY = 0;

    public Integer getItemsQuantity() {
        return this.items.size();
    }

    public Integer getProductsQuantity() {
        return this.items.stream()
                .map(ShoppingCartItem::getProductQuantity)
                .reduce(Integer::sum)
                .orElse(DEFAULT_PRODUCTS_QUANTITY);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        ShoppingCart that = (ShoppingCart) object;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ShoppingCart {" +
                "id = " + id +
                '}';
    }
}