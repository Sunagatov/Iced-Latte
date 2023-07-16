package com.zufar.onlinestore.payment.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String paymentId;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    private String status;

    private String description;


    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment payment)) return false;
        return Objects.equals(paymentId, payment.paymentId) && Objects.equals(currency, payment.currency) && Objects.equals(totalPrice, payment.totalPrice) && status == payment.status && Objects.equals(description, payment.description);
    }

    public int hashCode() {
        return Objects.hash(paymentId, currency, totalPrice, status, description);
    }

    @Override
    public String toString() {
        return "Payment{" +
                "paymentId='" + paymentId + '\'' +
                ", currency='" + currency + '\'' +
                ", amount=" + totalPrice +
                ", status=" + status +
                ", description='" + description + '\'' +
                '}';
    }
}
