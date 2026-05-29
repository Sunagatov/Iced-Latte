package com.zufar.icedlatte.user.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.*;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "address")
public class Address implements Serializable {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID addressId;

    @Column(name = "country", nullable = false, length = 128)
    private String country;

    @Column(name = "city", nullable = false, length = 128)
    private String city;

    @Column(name = "line", nullable = false, length = 128)
    private String line;

    @Column(name = "postcode", nullable = false, length = 128)
    private String postcode;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address address)) return false;
        return Objects.equals(addressId, address.addressId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addressId);
    }
}
