package com.zufar.onlinestore.payment.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * This record responsible for displaying the Stripe Api configuration in Spring as a bean.
 * Configuration stores special keys with which Stripe controls data security.
 * @param secretKey used to security work with the Stripe Api from the backend side.
 *
 * @param publishableKey used to security work with the Stripe Api from the frontend side.
 *
 * @param webHookSecretKey used to security work with the Stripe Api from the webhooks side.
 * */

@ConfigurationProperties(prefix = "stripe")
public record StripeConfiguration(String secretKey,
                                  String publishableKey,
                                  String webHookSecretKey) {

    @PostConstruct
    private void init() {
        Stripe.apiKey = secretKey;
    }
}
