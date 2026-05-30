package com.zufar.icedlatte.security.config;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

import com.zufar.icedlatte.common.http.ApiPaths;

@Component
public class SecurityRouteAuthorization {

    private static final String STRIPE_WEBHOOK_URL = ApiPaths.PAYMENT + "/stripe/webhook";
    private static final String SHIPPING_URL_PATTERN = "/api/v1/shipping/**";
    private static final String PRODUCT_REVIEW_URL_PATTERN = ApiPaths.PRODUCTS + "/*/review";
    private static final String PRODUCT_REVIEWS_URL_PATTERN = ApiPaths.PRODUCTS + "/*/reviews";
    private static final String PRODUCT_REVIEW_ITEM_URL_PATTERN = ApiPaths.PRODUCTS + "/*/reviews/*";
    private static final String PRODUCT_REVIEW_LIKES_URL_PATTERN = ApiPaths.PRODUCTS + "/*/reviews/*/likes";
    private static final String PRODUCT_REVIEWS_STATISTICS_URL_PATTERN = ApiPaths.PRODUCTS + "/*/reviews/statistics";
    private static final String AUTH_REGISTER_URL = ApiPaths.AUTH + "/register";
    private static final String AUTH_CONFIRM_URL = ApiPaths.AUTH + "/confirm";
    private static final String AUTH_LOGOUT_URL = ApiPaths.AUTH + "/logout";
    private static final String AUTH_PASSWORD_FORGOT_URL = ApiPaths.AUTH + "/password/forgot";
    private static final String AUTH_PASSWORD_CHANGE_URL = ApiPaths.AUTH + "/password/change";
    private static final String AUTH_OAUTH_TOKEN_URL = ApiPaths.AUTH_OAUTH + "/token";
    private static final String AUTH_OAUTH_PROVIDER_PATTERN = ApiPaths.AUTH_OAUTH + "/*";
    private static final String AUTH_OAUTH_CALLBACK_PATTERN = ApiPaths.AUTH_OAUTH + "/*/callback";

    public void authorize(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers(ApiPaths.AUTH_SESSIONS_PATTERN)
                .authenticated()
                .requestMatchers(ApiPaths.AUTH_LOGOUT_ALL)
                .authenticated()
                .requestMatchers(ApiPaths.CART_PATTERN)
                .authenticated()
                .requestMatchers(STRIPE_WEBHOOK_URL)
                .permitAll()
                .requestMatchers(ApiPaths.PAYMENT_PATTERN)
                .authenticated()
                .requestMatchers(ApiPaths.USERS_PATTERN)
                .authenticated()
                .requestMatchers(ApiPaths.FAVORITES_PATTERN)
                .authenticated()
                .requestMatchers(ApiPaths.ORDERS_PATTERN)
                .authenticated()
                .requestMatchers(SHIPPING_URL_PATTERN)
                .authenticated()
                .requestMatchers(PRODUCT_REVIEW_URL_PATTERN)
                .authenticated()
                .requestMatchers(HttpMethod.POST, PRODUCT_REVIEWS_URL_PATTERN)
                .authenticated()
                .requestMatchers(HttpMethod.DELETE, PRODUCT_REVIEW_ITEM_URL_PATTERN)
                .authenticated()
                .requestMatchers(HttpMethod.POST, PRODUCT_REVIEW_LIKES_URL_PATTERN)
                .authenticated()
                .requestMatchers(HttpMethod.GET, PRODUCT_REVIEWS_URL_PATTERN, PRODUCT_REVIEWS_STATISTICS_URL_PATTERN)
                .permitAll()
                .requestMatchers(
                        AUTH_REGISTER_URL,
                        AUTH_CONFIRM_URL,
                        ApiPaths.AUTH_AUTHENTICATE,
                        ApiPaths.AUTH_REFRESH,
                        AUTH_LOGOUT_URL,
                        AUTH_PASSWORD_FORGOT_URL,
                        AUTH_PASSWORD_CHANGE_URL,
                        AUTH_OAUTH_TOKEN_URL,
                        AUTH_OAUTH_PROVIDER_PATTERN,
                        AUTH_OAUTH_CALLBACK_PATTERN)
                .permitAll()
                .requestMatchers(ApiPaths.PRODUCTS_PATTERN)
                .permitAll()
                .requestMatchers(ApiPaths.DOCS_ROOT + "**")
                .permitAll()
                .requestMatchers("/actuator/health", "/actuator/info", "/livez", "/readyz")
                .permitAll()
                .requestMatchers(ApiPaths.ACTUATOR_ROOT + "**")
                .hasRole("ADMIN")
                .requestMatchers(ApiPaths.ADMIN_ORDERS_PATTERN)
                .hasRole("ADMIN")
                .requestMatchers(ApiPaths.API_ROOT + "/**")
                .authenticated()
                .anyRequest()
                .denyAll();
    }
}
