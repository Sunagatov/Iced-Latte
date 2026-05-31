package com.zufar.icedlatte.payment.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("") String secretKey,
        @DefaultValue("") String webhookSecret,
        @DefaultValue("usd") String currency,
        @DefaultValue List<ShippingOption> shippingOptions) {

    public StripeProperties {
        if (enabled && !StringUtils.hasText(secretKey)) {
            throw new IllegalStateException("stripe.secret-key must be configured when stripe.enabled=true.");
        }
        if (enabled && !StringUtils.hasText(webhookSecret)) {
            throw new IllegalStateException("stripe.webhook-secret must be configured when stripe.enabled=true.");
        }
    }

    public record ShippingOption(String name, long amountCents, long minDays, long maxDays) {}
}
