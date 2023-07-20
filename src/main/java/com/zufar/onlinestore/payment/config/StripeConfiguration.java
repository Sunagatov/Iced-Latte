package com.zufar.onlinestore.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * This record responsible for displaying the Stripe Api configuration in Spring as a bean.
 * Configuration stores special keys with which Stripe controls data security.
 * @param secretKey used to work with the Stripe Api from the backend side.
 * @param publishableKey used to work with the Stripe Api from the frontend side.
 * */
@ConfigurationProperties(prefix = "stripe")
public record StripeConfiguration(String secretKey, String publishableKey) {}
