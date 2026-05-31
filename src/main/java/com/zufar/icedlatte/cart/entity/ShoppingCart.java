package com.zufar.icedlatte.cart.entity;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;

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
            fetch = FetchType.LAZY)
    private Set<ShoppingCartItem> items;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || Hibernate.getClass(this) != Hibernate.getClass(object)) return false;
        ShoppingCart that = (ShoppingCart) object;
        if (id == null || that.id == null) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
