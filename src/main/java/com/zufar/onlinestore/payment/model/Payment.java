package com.zufar.onlinestore.payment.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Getter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import java.math.BigDecimal;

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

    @Column(nullable = false, name = "currency")
    private String currency;

    @Column(nullable = false, name = "items_total_price")
    private BigDecimal itemsTotalPrice;

    @Column(name = "status")
    private String status;

    @Column(name = "description")
    private String description;

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Payment)) {
            return false;
        }
        Payment that = (Payment) obj;
        EqualsBuilder eb = new EqualsBuilder();
        eb.append(paymentId, that.paymentId);
        return eb.isEquals();
    }

    public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder();
        hcb.append(paymentId);
        return hcb.toHashCode();
    }

    public String toString() {
        return new ToStringBuilder(this)
                .append("paymentId", paymentId)
                .append("currency", currency)
                .append("itemsTotalPrice", itemsTotalPrice)
                .append("status", status)
                .append("description", description)
                .toString();
    }
}
