package com.zufar.icedlatte.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "address")
public class OrderAddress implements Serializable {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID addressId;

    @Column(name = "country", nullable = false, length = 55)
    private String country;

    @Column(name = "city", nullable = false, length = 55)
    private String city;

    @Column(name = "line", nullable = false, length = 55)
    private String line;

    @Column(name = "postcode", nullable = false, length = 55)
    private String postcode;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderAddress that)) return false;
        return Objects.equals(addressId, that.addressId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addressId);
    }
}
