package com.zufar.onlinestore.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@Table(name = "product")
public class ProductInfo {

    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "currency")
    private String currency;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "active")
    private boolean active;

    public ProductInfo(UUID id,
                       String name,
                       String description,
                       BigDecimal price,
                       String currency,
                       Integer quantity,
                       boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.quantity = quantity;
        this.active = active;
    }

    public ProductInfo() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductInfo that = (ProductInfo) o;
        return active == that.active &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(price, that.price) &&
                Objects.equals(currency, that.currency) &&
                Objects.equals(quantity, that.quantity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, price, currency, quantity, active);
    }

    @Override
    public String toString() {
        return "ProductInfo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", currency='" + currency + '\'' +
                ", quantity=" + quantity +
                ", active=" + active +
                '}';
    }
}