package com.zufar.onlinestore.cart.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shopping_session")
public class ShoppingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToMany(mappedBy = "shoppingSession",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, //TODO Think about CascadeType and choose appropriate one
                    CascadeType.REFRESH, CascadeType.REMOVE, CascadeType.DETACH},
            orphanRemoval = true,
            fetch = FetchType.EAGER) //TODO Think about FetchType better
    private Set<ShoppingSessionItem> items;

    @Column(name = "items_quantity", nullable = false)
    private Integer itemsQuantity;

    @Column(name = "products_quantity", nullable = false)
    private Integer productsQuantity;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at", nullable = false)
    private LocalDateTime closedAt;

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        ShoppingSession that = (ShoppingSession) object;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ShoppingSession {" +
                "id = " + id +
                '}';
    }
}