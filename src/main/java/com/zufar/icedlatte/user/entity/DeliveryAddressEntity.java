package com.zufar.icedlatte.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Objects;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "delivery_address")
public class DeliveryAddressEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "label", nullable = false, length = 64)
    private String label;

    @Column(name = "line", nullable = false, length = 256)
    private String line;

    @Column(name = "city", nullable = false, length = 128)
    private String city;

    @Column(name = "country", nullable = false, length = 128)
    private String country;

    @Column(name = "postcode", nullable = false, length = 16)
    private String postcode;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryAddressEntity that = (DeliveryAddressEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
