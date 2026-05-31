package com.zufar.icedlatte.security.config;

import static org.springframework.security.config.Customizer.withDefaults;

import java.time.Duration;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

import com.zufar.icedlatte.common.correlation.CorrelationFilter;
import com.zufar.icedlatte.common.exception.ProblemType;
import com.zufar.icedlatte.security.jwt.filter.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SpringSecurityConfiguration {

    private final SecurityRouteAuthorization routeAuthorization;
    private final SecurityProblemResponseWriter problemResponseWriter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            final HttpSecurity httpSecurity,
            final CorrelationFilter correlationFilter,
            final JwtAuthenticationFilter jwtTokenFilter,
            @Qualifier("rateLimitingFilter") final Filter rateLimitingFilter,
            final CorsConfigurationSource corsConfigurationSource) {
        return httpSecurity
                // amazonq-ignore-next-line
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                                .maxAgeInSeconds(Duration.ofDays(365).toSeconds())
                                .includeSubDomains(true)
                                .preload(true))
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .contentTypeOptions(withDefaults()))
                .authorizeHttpRequests(routeAuthorization::authorize)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, _) -> writeErrorResponse(
                                response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Authentication required.",
                                request.getRequestURI()))
                        .accessDeniedHandler((request, response, _) -> writeErrorResponse(
                                response, HttpServletResponse.SC_FORBIDDEN, "Access denied.", request.getRequestURI())))
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

    @Bean
    public FilterRegistrationBean<Filter> rateLimitingFilterRegistration(
            @Qualifier("rateLimitingFilter") Filter filter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    // amazonq-ignore-next-line
    @Bean
    public AuthenticationProvider authenticationProvider(
            final UserDetailsService userDetailsService, final PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        // amazonq-ignore-next-line
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        authenticationProvider.setHideUserNotFoundExceptions(true);
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

    private void writeErrorResponse(HttpServletResponse response, int status, String detail, String path)
            throws java.io.IOException {
        String typeSlug = status == 401 ? ProblemType.AUTH_REQUIRED : ProblemType.ACCESS_DENIED;
        String title = status == 401 ? "Authentication required" : "Access denied";
        problemResponseWriter.write(response, status, typeSlug, title, detail, path, null);
    }
}
