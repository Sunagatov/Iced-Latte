package com.zufar.icedlatte.payment.entity;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Embeddable
@Getter
@Setter
public class ShippingAddress {

    @NotNull
    private String country;

    @NotNull
    private String addressLine;

    @NotNull
    private String city;

    @NotNull
    private String zipCode;
}
