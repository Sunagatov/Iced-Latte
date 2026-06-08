package com.zufar.icedlatte.security.config;

import jakarta.servlet.DispatcherType;

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

    private static final String[] AUTHENTICATED_URL_PATTERNS = {
        ApiPaths.AUTH_SESSIONS_PATTERN,
        ApiPaths.AUTH_LOGOUT_ALL,
        ApiPaths.CART_PATTERN,
        ApiPaths.USERS_PATTERN,
        ApiPaths.FAVORITES_PATTERN,
        ApiPaths.ORDERS_PATTERN,
        ApiPaths.SUPPORT_CHAT_PATTERN,
        SHIPPING_URL_PATTERN
    };

    private static final String[] PUBLIC_AUTH_URL_PATTERNS = {
        AUTH_REGISTER_URL,
        AUTH_CONFIRM_URL,
        ApiPaths.AUTH_AUTHENTICATE,
        ApiPaths.AUTH_REFRESH,
        AUTH_LOGOUT_URL,
        AUTH_PASSWORD_FORGOT_URL,
        AUTH_PASSWORD_CHANGE_URL,
        AUTH_OAUTH_TOKEN_URL,
        AUTH_OAUTH_PROVIDER_PATTERN,
        AUTH_OAUTH_CALLBACK_PATTERN
    };

    private static final String[] PUBLIC_HEALTH_URL_PATTERNS = {
        "/actuator/health", "/actuator/info", "/livez", "/readyz"
    };

    public void authorize(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();

        authorizeAuthenticatedRoutes(auth);
        authorizePaymentRoutes(auth);
        authorizeProductReviewRoutes(auth);
        authorizePublicRoutes(auth);
        authorizeAdminRoutes(auth);
        authorizeFallbackRoutes(auth);
    }

    private void authorizeAuthenticatedRoutes(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers(AUTHENTICATED_URL_PATTERNS).authenticated();
    }

    private void authorizePaymentRoutes(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers(STRIPE_WEBHOOK_URL)
                .permitAll()
                .requestMatchers(ApiPaths.PAYMENT_PATTERN)
                .authenticated();
    }

    private void authorizeProductReviewRoutes(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers(PRODUCT_REVIEW_URL_PATTERN)
                .authenticated()
                .requestMatchers(HttpMethod.POST, PRODUCT_REVIEWS_URL_PATTERN)
                .authenticated()
                .requestMatchers(HttpMethod.DELETE, PRODUCT_REVIEW_ITEM_URL_PATTERN)
                .authenticated()
                .requestMatchers(HttpMethod.POST, PRODUCT_REVIEW_LIKES_URL_PATTERN)
                .authenticated()
                .requestMatchers(HttpMethod.GET, PRODUCT_REVIEWS_URL_PATTERN, PRODUCT_REVIEWS_STATISTICS_URL_PATTERN)
                .permitAll();
    }

    private void authorizePublicRoutes(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers(PUBLIC_AUTH_URL_PATTERNS)
                .permitAll()
                .requestMatchers(ApiPaths.PRODUCTS_PATTERN)
                .permitAll()
                .requestMatchers(ApiPaths.DOCS_ROOT + "**")
                .permitAll()
                .requestMatchers(PUBLIC_HEALTH_URL_PATTERNS)
                .permitAll();
    }

    private void authorizeAdminRoutes(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers(ApiPaths.ACTUATOR_ROOT + "**")
                .hasRole("ADMIN")
                .requestMatchers(ApiPaths.ADMIN_ORDERS_PATTERN)
                .hasRole("ADMIN");
    }

    private void authorizeFallbackRoutes(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers(ApiPaths.API_ROOT + "/**")
                .authenticated()
                .anyRequest()
                .denyAll();
    }
}
