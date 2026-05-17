package com.zufar.icedlatte.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "order_item")
@SuppressWarnings("unused") // JPA reads and writes entity fields reflectively.
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "product_price")
    private BigDecimal productPrice;

    @Column(name = "product_name", length = 64)
    private String productName;

    @Column(name = "products_quantity", nullable = false)
    private Integer productsQuantity;
}
