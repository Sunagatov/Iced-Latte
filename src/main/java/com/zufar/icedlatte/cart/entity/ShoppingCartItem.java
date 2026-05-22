package com.zufar.icedlatte.cart.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "shopping_cart_item")
public class ShoppingCartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @Version
    @Builder.Default
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopping_cart_id", nullable = false)
    private ShoppingCart shoppingCart;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "products_quantity", nullable = false)
    private Integer productQuantity;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ShoppingCartItem that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, productId);
    }
}
