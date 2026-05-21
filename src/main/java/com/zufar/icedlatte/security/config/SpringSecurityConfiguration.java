package com.zufar.icedlatte.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zufar.icedlatte.common.correlation.CorrelationFilter;
import com.zufar.icedlatte.common.exception.ProblemType;
import com.zufar.icedlatte.common.exception.handler.ProblemTypeUriFactory;
import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.security.service.jwt.filter.JwtAuthenticationFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.DisableEncodeUrlFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Duration;
import java.time.Instant;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SpringSecurityConfiguration {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String STRIPE_WEBHOOK_URL = ApiPaths.PAYMENT + "/stripe/webhook";
    private static final String SHIPPING_URL_PATTERN = "/api/v1/shipping/**";
    private static final String PRODUCT_REVIEW_URL_PATTERN = ApiPaths.PRODUCTS + "/*/review";
    private static final String PRODUCT_REVIEWS_URL_PATTERN = ApiPaths.PRODUCTS + "/*/reviews";
    private static final String PRODUCT_REVIEW_ITEM_URL_PATTERN = ApiPaths.PRODUCTS + "/*/reviews/*";
    private static final String PRODUCT_REVIEW_LIKES_URL_PATTERN = ApiPaths.PRODUCTS + "/*/reviews/*/likes";
    private static final String PRODUCT_REVIEWS_STATISTICS_URL_PATTERN = ApiPaths.PRODUCTS + "/*/reviews/statistics";

    private final ProblemTypeUriFactory problemTypeUriFactory;

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity httpSecurity,
                                                   final CorrelationFilter correlationFilter,
                                                   final JwtAuthenticationFilter jwtTokenFilter,
                                                   @Qualifier("rateLimitingFilter") final Filter rateLimitingFilter,
                                                   final CorsConfigurationSource corsConfigurationSource) {
        return httpSecurity
                // amazonq-ignore-next-line
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                                .maxAgeInSeconds(Duration.ofDays(365).toSeconds())
                                .includeSubDomains(true)
                                .preload(true))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .contentTypeOptions(withDefaults())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ApiPaths.AUTH_SESSIONS_PATTERN).authenticated()
                        .requestMatchers(ApiPaths.AUTH_LOGOUT_ALL).authenticated()
                        .requestMatchers(ApiPaths.CART_PATTERN).authenticated()
                        .requestMatchers(STRIPE_WEBHOOK_URL).permitAll()
                        .requestMatchers(ApiPaths.PAYMENT_PATTERN).authenticated()
                        .requestMatchers(ApiPaths.USERS_PATTERN).authenticated()
                        .requestMatchers(ApiPaths.FAVORITES_PATTERN).authenticated()
                        .requestMatchers(ApiPaths.ORDERS_PATTERN).authenticated()
                        .requestMatchers(SHIPPING_URL_PATTERN).authenticated()
                        .requestMatchers(PRODUCT_REVIEW_URL_PATTERN).authenticated()
                        .requestMatchers(HttpMethod.POST, PRODUCT_REVIEWS_URL_PATTERN).authenticated()
                        .requestMatchers(HttpMethod.DELETE, PRODUCT_REVIEW_ITEM_URL_PATTERN).authenticated()
                        .requestMatchers(HttpMethod.POST, PRODUCT_REVIEW_LIKES_URL_PATTERN).authenticated()
                        .requestMatchers(HttpMethod.GET, PRODUCT_REVIEWS_URL_PATTERN, PRODUCT_REVIEWS_STATISTICS_URL_PATTERN).permitAll()
                        .requestMatchers(HttpMethod.GET, ApiPaths.AUTH_ALL_PATTERN).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers(ApiPaths.ACTUATOR_ROOT + "**").hasRole("ADMIN")
                        .requestMatchers(ApiPaths.ADMIN_ORDERS_PATTERN).hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, _) ->
                                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        "Authentication required.", request.getRequestURI()))
                        .accessDeniedHandler((request, response, _) ->
                                writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                                        "Access denied.", request.getRequestURI()))
                )
                .addFilterBefore(correlationFilter, DisableEncodeUrlFilter.class)
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAt(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public FilterRegistrationBean<CorrelationFilter> correlationFilterRegistration(CorrelationFilter filter) {
        FilterRegistrationBean<CorrelationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    // amazonq-ignore-next-line
    @Bean
    public AuthenticationProvider authenticationProvider(final UserDetailsService userDetailsService,
                                                         final PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        // amazonq-ignore-next-line
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        authenticationProvider.setHideUserNotFoundExceptions(false);
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(final AuthenticationConfiguration authenticationConfiguration) {
        try {
            return authenticationConfiguration.getAuthenticationManager();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build AuthenticationManager", e);
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder(
            @Value("${security.argon2.memory:16384}") int memory,
            @Value("${security.argon2.iterations:2}") int iterations) {
        if (memory < 1024) {
            throw new IllegalArgumentException("security.argon2.memory must be at least 1024 KB, got: " + memory);
        }
        if (iterations < 1) {
            throw new IllegalArgumentException("security.argon2.iterations must be at least 1, got: " + iterations);
        }
        return new Argon2PasswordEncoder(16, 32, 1, memory, iterations);
    }

    private void writeErrorResponse(HttpServletResponse response,
                                    int status,
                                    String message,
                                    String path) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String typeSlug = status == 401 ? ProblemType.AUTH_REQUIRED : ProblemType.ACCESS_DENIED;
        String title = status == 401 ? "Authentication required" : "Access denied";
        ObjectNode json = OBJECT_MAPPER.createObjectNode()
                .put("type", problemTypeUriFactory.build(typeSlug))
                .put("title", title)
                .put("status", status)
                .put("detail", message)
                .put("instance", path)
                .put("timestamp", Instant.now().toString());
        byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(json);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }
}
