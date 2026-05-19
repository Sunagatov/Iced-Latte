package com.zufar.icedlatte.security.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS configuration for enhanced security.
 * Uses environment-specific settings for different deployment scenarios.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AppCorsConfiguration {

    private final CorsProperties corsProperties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(corsProperties.allowedMethods());

        if (corsProperties.allowedHeaders().size() == 1 && "*".equals(corsProperties.allowedHeaders().getFirst())) {
            configuration.addAllowedHeader("*");
        } else {
            configuration.setAllowedHeaders(corsProperties.allowedHeaders());
        }

        configuration.setExposedHeaders(corsProperties.exposedHeaders());
        configuration.setAllowCredentials(corsProperties.allowCredentials());
        configuration.setMaxAge(corsProperties.maxAge());

        log.debug("cors.config.initialized: origins={}, methods={}, allowCredentials={}",
                corsProperties.allowedOrigins(), corsProperties.allowedMethods(), corsProperties.allowCredentials());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
