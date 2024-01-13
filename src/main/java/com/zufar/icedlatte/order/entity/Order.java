package com.zufar.icedlatte.order.entity;

import com.zufar.icedlatte.openapi.dto.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "order",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE,
                    CascadeType.REFRESH, CascadeType.REMOVE, CascadeType.DETACH},
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    private Set<OrderItem> items;

    @Column(name = "items_quantity", nullable = false)
    private Integer itemsQuantity;

    @Column(name = "total_products_cost", nullable = false)
    private BigDecimal totalProductsCost; // TODO: should we store this in DB?

    @Column(name = "delivery_cost", nullable = false)
    private BigDecimal deliveryCost;

    @Column(name = "tax_cost", nullable = false)
    private BigDecimal taxCost;

    @Column(name = "total_order_cost", nullable = false)
    private BigDecimal totalOrderCost; // TODO: should we store this in DB?

    @Column(name = "delivery_info", nullable = false)
    private String deliveryInfo;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "recipient_surname", nullable = false)
    private String recipientSurname;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (Order) object;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Order {" +
                "id=" + id +
                ", user=" + userId +
                ", items=" + items +
                '}';
    }
}
