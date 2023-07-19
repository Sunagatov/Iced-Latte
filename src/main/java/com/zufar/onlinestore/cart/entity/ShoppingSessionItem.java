package com.zufar.onlinestore.cart.entity;

import com.zufar.onlinestore.product.entity.ProductInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shopping_session_item")
public class ShoppingSessionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private ShoppingSession cart;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private ProductInfo productInfo;

    @Column(name = "products_quantity", nullable = false)
    private Integer productsQuantity;

    public ShoppingSessionItem(UUID id, ShoppingSession cart, ProductInfo productInfo, Integer productsQuantity) {
        this.id = id;
        this.cart = cart;
        this.productInfo = productInfo;
        this.productsQuantity = productsQuantity;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        ShoppingSessionItem that = (ShoppingSessionItem) object;
        return Objects.equals(id, that.id) &&
                Objects.equals(cart, that.cart) &&
                Objects.equals(productInfo, that.productInfo) &&
                Objects.equals(productsQuantity, that.productsQuantity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, cart, productInfo, productsQuantity);
    }

    @Override
    public String toString() {
        return "ShoppingSessionItem{" +
                "id=" + id +
                ", cart=" + cart +
                ", productInfo=" + productInfo +
                ", productsQuantity=" + productsQuantity +
                '}';
    }
}