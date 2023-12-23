package com.zufar.icedlatte.payment.entity;

import com.zufar.icedlatte.user.entity.UserEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Column;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Embedded;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Shipping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long shippingId;

    @Column(name = "shipping_user_email", nullable = false)
    private String shippingUserEmail;

    @Column(name = "shipping_user_first_name", nullable = false)
    private String shippingUserFirstName;

    @Column(name = "shipping_user_last_name", nullable = false)
    private String shippingUserLastName;

    @Column(name = "shipping_user_phone_number", nullable = false)
    private String shippingUserPhoneNumber;

    @Column(name = "shipping_method", nullable = false)
    private String shippingMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private UserEntity user;

    @Embedded
    private ShippingAddress shippingAddress;

}
