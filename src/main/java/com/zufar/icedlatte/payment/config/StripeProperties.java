package com.zufar.icedlatte.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(
        @DefaultValue("usd") String currency,
        @DefaultValue List<ShippingOption> shippingOptions
) {
    public record ShippingOption(
            String name,
            long amountCents,
            long minDays,
            long maxDays
    ) {}
}
