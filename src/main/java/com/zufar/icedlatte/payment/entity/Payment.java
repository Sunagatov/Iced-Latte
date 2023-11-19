package com.zufar.icedlatte.payment.entity;

import com.zufar.icedlatte.payment.enums.PaymentStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Getter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "payment_intent_id", nullable = false, unique = true)
    private String paymentIntentId;

    @Column(name = "shopping_cart_id", nullable = false, unique = true)
    private UUID shoppingCartId;

    @Column(name = "items_total_price", nullable = false)
    private BigDecimal itemsTotalPrice;

    @Column(name = "status", nullable = true)
    @Enumerated(value = EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "description", nullable = true)
    private String description;

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Payment payment)) {
            return false;
        }
        EqualsBuilder eb = new EqualsBuilder();
        eb.append(paymentIntentId, payment.paymentId);
        return eb.isEquals();
    }

    public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder();
        hcb.append(paymentIntentId);
        return hcb.toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("paymentIntentId", paymentIntentId)
                .toString();
    }
}
