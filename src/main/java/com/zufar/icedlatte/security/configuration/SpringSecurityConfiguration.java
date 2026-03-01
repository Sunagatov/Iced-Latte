package com.zufar.icedlatte.security.configuration;

import com.zufar.icedlatte.security.jwt.JwtAuthenticationFilter;
import com.zufar.icedlatte.security.filter.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.beans.factory.annotation.Value;
import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Duration;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SpringSecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity httpSecurity,
                                                   final JwtAuthenticationFilter jwtTokenFilter,
                                                   final RateLimitingFilter rateLimitingFilter,
                                                   final CorsConfigurationSource corsConfigurationSource) throws Exception {
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
                        .requestMatchers(SecurityConstants.SHOPPING_CART_URL).authenticated()
                        .requestMatchers(SecurityConstants.PAYMENT_URL).authenticated()
                        .requestMatchers(SecurityConstants.STRIPE_WEBHOOK_URL).permitAll()
                        .requestMatchers(SecurityConstants.USERS_URL).authenticated()
                        .requestMatchers(SecurityConstants.FAVOURITES_URL).authenticated()
                        .requestMatchers(SecurityConstants.ORDERS_URL).authenticated()
                        .requestMatchers(SecurityConstants.SHIPPING_URL).authenticated()
                        .requestMatchers(SecurityConstants.PRODUCT_REVIEW_URL).authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/*/reviews").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products/*/reviews/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/*/reviews/*/likes").authenticated()
                        .requestMatchers(HttpMethod.GET, SecurityConstants.ALLOWED_PRODUCT_REVIEWS_URLS.toArray(String[]::new)).permitAll()
                        .requestMatchers(HttpMethod.GET, SecurityConstants.AUTH_3PART_URL).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                )
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
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
}
