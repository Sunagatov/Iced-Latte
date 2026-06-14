package com.zufar.icedlatte.security.config.cors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** CORS configuration for enhanced security. Uses environment-specific settings for different deployment scenarios. */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AppCorsConfiguration {

    private final CorsProperties corsProperties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        validateCredentialsOrigins();

        CorsConfiguration configuration = buildCorsConfiguration();

        String logMessage = "cors.config.initialized: origins={}, methods={}, allowCredentials={}";
        log.debug(
                logMessage,
                corsProperties.allowedOrigins(),
                corsProperties.allowedMethods(),
                corsProperties.allowCredentials());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private CorsConfiguration buildCorsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(corsProperties.allowedMethods());

        if (corsProperties.allowedHeaders().size() == 1
                && "*".equals(corsProperties.allowedHeaders().getFirst())) {
            configuration.addAllowedHeader("*");
        } else {
            configuration.setAllowedHeaders(corsProperties.allowedHeaders());
        }

        configuration.setExposedHeaders(corsProperties.exposedHeaders());
        configuration.setAllowCredentials(corsProperties.allowCredentials());
        configuration.setMaxAge(corsProperties.maxAge());

        return configuration;
    }

    private void validateCredentialsOrigins() {
        if (!Boolean.TRUE.equals(corsProperties.allowCredentials())) {
            return;
        }
        corsProperties.allowedOrigins().stream()
                .filter(origin -> origin == null || origin.isBlank() || origin.contains("*"))
                .findFirst()
                .ifPresent(origin -> {
                    String errorMessage = "CORS allow-credentials=true requires explicit allowed origins, got: ";
                    throw new IllegalStateException(errorMessage + origin);
                });
    }
}
