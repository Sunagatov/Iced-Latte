package com.zufar.onlinestore.notification.entity;

import com.zufar.onlinestore.customer.entity.Customer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Alex Zarubin
 * created on 24.05.2023
 */


@Entity
@Builder
@Getter
@Setter
@Table(name = "notification")
public class Notification {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "message")
    private String message;

    @OneToOne(cascade = CascadeType.REFRESH)
    @JoinColumn(name = "recipient_id", referencedColumnName = "id")
    private Customer recipient;

    public Notification(UUID id, String message, Customer recipient) {
        this.id = id;
        this.message = message;
        this.recipient = recipient;
    }

    public Notification() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return Objects.equals(id, that.id) && Objects.equals(message, that.message) && Objects.equals(recipient, that.recipient);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, message, recipient);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", message='" + message + '\'' +
                ", recipient=" + recipient +
                '}';
    }
}