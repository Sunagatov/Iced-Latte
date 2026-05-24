package com.zufar.icedlatte.cart.entity;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Formula;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "shopping_cart")
public class ShoppingCart {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToMany(
            mappedBy = "shoppingCart",
            cascade = {
                CascadeType.PERSIST,
                CascadeType.MERGE,
                CascadeType.REFRESH,
                CascadeType.REMOVE,
                CascadeType.DETACH
            },
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    private Set<ShoppingCartItem> items;

    @Formula("(SELECT COUNT(*) FROM shopping_cart_item sci WHERE sci.shopping_cart_id = id)")
    private Integer itemsQuantity;

    @Formula(
            "(SELECT COALESCE(SUM(sci.products_quantity), 0) FROM shopping_cart_item sci WHERE sci.shopping_cart_id = id)")
    private Integer productsQuantity;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ShoppingCart that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
