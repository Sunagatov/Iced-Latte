package com.zufar.icedlatte.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

    private String currency = "usd";
    private List<ShippingOption> shippingOptions = List.of();

    @Getter
    @Setter
    public static class ShippingOption {
        private String name;
        private long amountCents;
        private long minDays;
        private long maxDays;
    }
}
