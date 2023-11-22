package com.zufar.icedlatte.cart.entity;

import com.zufar.icedlatte.product.entity.ProductInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.data.annotation.Version;

import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "shopping_cart_item")
public class ShoppingCartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Integer version; // Adding version field for optimistic locking

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopping_cart_id", nullable = false)
    private ShoppingCart shoppingCart;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductInfo productInfo;

    @Column(name = "products_quantity", nullable = false)
    private Integer productQuantity;

    public ShoppingCartItem(UUID id, ShoppingCart shoppingCart, ProductInfo productInfo, Integer productQuantity) {
        this.id = id;
        this.shoppingCart = shoppingCart;
        this.productInfo = productInfo;
        this.productQuantity = productQuantity;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;

        if (object == null || getClass() != object.getClass())
            return false;

        ShoppingCartItem that = (ShoppingCartItem) object;

        return new EqualsBuilder()
                .append(id, that.id)
                .append(productInfo, that.productInfo)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(productInfo)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "ShoppingCartItem {" +
                "id = " + id +
                '}';
    }
}