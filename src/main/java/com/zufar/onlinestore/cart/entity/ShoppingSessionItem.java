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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shopping_session_item")
public class ShoppingSessionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "shopping_session_id", nullable = false)
    private ShoppingSession shoppingSession;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private ProductInfo productInfo;

    @Column(name = "products_quantity", nullable = false)
    private Integer productsQuantity;

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        ShoppingSessionItem that = (ShoppingSessionItem) object;
        return Objects.equals(id, that.id) &&
                Objects.equals(shoppingSession, that.shoppingSession) &&
                Objects.equals(productInfo, that.productInfo) &&
                Objects.equals(productsQuantity, that.productsQuantity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, shoppingSession, productInfo, productsQuantity);
    }

    @Override
    public String toString() {
        return "ShoppingSessionItem{" +
                "id=" + id +
                ", shoppingSession=" + shoppingSession +
                ", productInfo=" + productInfo +
                ", productsQuantity=" + productsQuantity +
                '}';
    }
}