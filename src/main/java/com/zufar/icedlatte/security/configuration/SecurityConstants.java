package com.zufar.icedlatte.security.configuration;

import com.zufar.icedlatte.common.http.ApiPaths;
import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public final class SecurityConstants {

    public static final String ANONYMOUS_PRINCIPAL = "anonymousUser";
    public static final String AUTH_SESSION_URL = AuthPaths.SESSIONS_PATTERN;
    public static final String AUTH_LOGOUT_ALL_URL = AuthPaths.LOGOUT_ALL;
    public static final String SHOPPING_CART_URL = ApiPaths.CART_PATTERN;
    public static final String PAYMENT_URL = ApiPaths.PAYMENT_PATTERN;
    public static final String STRIPE_WEBHOOK_URL = ApiPaths.PAYMENT + "/stripe/webhook";
    public static final String USERS_URL = ApiPaths.USERS_PATTERN;
    public static final String FAVOURITES_URL = ApiPaths.FAVORITES_PATTERN;
    public static final String ORDERS_URL = ApiPaths.ORDERS_PATTERN;
    public static final String PRODUCT_REVIEW_URL = "/api/v1/products/*/review";
    public static final Set<String> ALLOWED_PRODUCT_REVIEWS_URLS =
            Set.of("/api/v1/products/*/reviews", "/api/v1/products/*/reviews/statistics");
    public static final String SHIPPING_URL = "/api/v1/shipping/**";
    public static final String AUTH_3PART_URL = AuthPaths.ALL_PATTERN;
}
