package com.zufar.onlinestore.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@Table(name = "address")
public class Address {

    @Id
    @Column(unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "line", nullable = false)
    private String line;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "country", nullable = false)
    private String country;

    public Address(UUID id, String line, String city, String country) {
        this.id = id;
        this.line = line;
        this.city = city;
        this.country = country;
    }

    public Address() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(id, address.id) && Objects.equals(line, address.line) && Objects.equals(city, address.city) && Objects.equals(country, address.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, line, city, country);
    }

    @Override
    public String toString() {
        return "Address{" +
                "id=" + id +
                ", line='" + line + '\'' +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}