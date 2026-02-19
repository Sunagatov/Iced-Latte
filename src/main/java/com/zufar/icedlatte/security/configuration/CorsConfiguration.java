package com.zufar.icedlatte.security.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for enhanced security.
 * Uses environment-specific settings for different deployment scenarios.
 */
@Slf4j
@Configuration
public class CorsConfiguration {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${cors.exposed-headers:Authorization,Content-Type,X-Request-ID}")
    private String exposedHeaders;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        
        // Parse allowed origins
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOriginPatterns(origins);
        
        // Parse allowed methods
        List<String> methods = Arrays.asList(allowedMethods.split(","));
        configuration.setAllowedMethods(methods);
        
        // Parse allowed headers
        if (!"*".equals(allowedHeaders)) {
            List<String> headers = Arrays.asList(allowedHeaders.split(","));
            configuration.setAllowedHeaders(headers);
        } else {
            configuration.addAllowedHeader("*");
        }
        
        // Parse exposed headers
        List<String> exposed = Arrays.asList(exposedHeaders.split(","));
        configuration.setExposedHeaders(exposed);
        
        // Security settings
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(maxAge);
        
        // Additional security headers
        configuration.addExposedHeader("X-Request-ID");
        configuration.addExposedHeader("X-Rate-Limit-Remaining");
        configuration.addExposedHeader("X-Rate-Limit-Reset");
        
        log.info("CORS configuration initialized with origins: {}, methods: {}, allowCredentials: {}", 
                origins, methods, allowCredentials);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }
}
