package com.zufar.icedlatte.common.http;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ApiPaths {

    public static final String API_ROOT = "/api";
    public static final String USERS = "/api/v1/users";
    public static final String CART = "/api/v1/cart";
    public static final String FAVORITES = "/api/v1/favorites";
    public static final String PRODUCTS = "/api/v1/products";
    public static final String ORDERS = "/api/v1/orders";
    public static final String ADMIN_ORDERS = "/api/v1/admin/orders";
    public static final String PAYMENT = "/api/v1/payment";
    public static final String DOCS_ROOT = "/api/docs/";
    public static final String ACTUATOR_ROOT = "/actuator/";

    public static final String USERS_PATTERN = USERS + "/**";
    public static final String CART_PATTERN = CART + "/**";
    public static final String FAVORITES_PATTERN = FAVORITES + "/**";
    public static final String ORDERS_PATTERN = ORDERS + "/**";
    public static final String ADMIN_ORDERS_PATTERN = ADMIN_ORDERS + "/**";
    public static final String PAYMENT_PATTERN = PAYMENT + "/**";

    public static final String PRODUCTS_BRANDS = PRODUCTS + "/brands";
    public static final String PRODUCTS_SELLERS = PRODUCTS + "/sellers";

    public static final String USERS_PASSWORD_RESET = USERS + "/password/reset";
    public static final String USERS_PASSWORD_RESET_CONFIRM = USERS + "/password/reset/confirm";
}
