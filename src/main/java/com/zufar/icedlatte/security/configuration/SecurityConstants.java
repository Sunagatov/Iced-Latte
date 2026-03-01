package com.zufar.icedlatte.security.configuration;

import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public final class SecurityConstants {

    public static final String SHOPPING_CART_URL = "/api/v1/cart/**";
    public static final String PAYMENT_URL = "/api/v1/payment/**";
    public static final String STRIPE_WEBHOOK_URL = "/api/v1/payment/stripe/webhook";
    public static final String USERS_URL = "/api/v1/users/**";
    public static final String FAVOURITES_URL = "/api/v1/favorites/**";
    public static final String ORDERS_URL = "/api/v1/orders/**";
    public static final String PRODUCT_REVIEW_URL = "/api/v1/products/*/review";
    public static final Set<String> ALLOWED_PRODUCT_REVIEWS_URLS = Set.of("/api/v1/products/*/reviews", "/api/v1/products/*/reviews/statistics");
    public static final String SHIPPING_URL = "/api/v1/shipping/**";
    public static final String AUTH_3PART_URL = "/api/v1/auth/**";
}
