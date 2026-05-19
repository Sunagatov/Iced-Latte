package com.zufar.icedlatte.security.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AppCorsConfiguration unit tests")
class AppCorsConfigurationTest {

    @Test
    @DisplayName("builds API CORS config with wildcard allowed header support")
    void buildsCorsConfigWithWildcardAllowedHeader() {
        var properties = new CorsProperties(
                List.of("https://app.example.com"),
                List.of("GET", "POST"),
                List.of("*"),
                List.of("Authorization", "X-Trace-ID"),
                true,
                7200L);
        AppCorsConfiguration configuration = new AppCorsConfiguration(properties);

        CorsConfigurationSource source = configuration.corsConfigurationSource();
        CorsConfiguration corsConfiguration = source.getCorsConfiguration(
                new MockHttpServletRequest("GET", "/api/v1/products"));

        assertThat(corsConfiguration).isNotNull();
        assertThat(corsConfiguration.getAllowedOriginPatterns()).containsExactly("https://app.example.com");
        assertThat(corsConfiguration.getAllowedMethods()).containsExactly("GET", "POST");
        assertThat(corsConfiguration.getAllowedHeaders()).containsExactly("*");
        assertThat(corsConfiguration.getExposedHeaders()).containsExactly("Authorization", "X-Trace-ID");
        assertThat(corsConfiguration.getAllowCredentials()).isTrue();
        assertThat(corsConfiguration.getMaxAge()).isEqualTo(7200L);
    }

    @Test
    @DisplayName("uses explicit allowed headers list when wildcard is not configured")
    void usesExplicitAllowedHeadersList() {
        var properties = new CorsProperties(
                List.of("https://app.example.com"),
                List.of("GET"),
                List.of("Authorization", "Content-Type"),
                List.of("X-Session-ID"),
                false,
                3600L);
        AppCorsConfiguration configuration = new AppCorsConfiguration(properties);

        CorsConfigurationSource source = configuration.corsConfigurationSource();
        CorsConfiguration corsConfiguration =
                source.getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/v1/auth/authenticate"));

        assertThat(corsConfiguration).isNotNull();
        assertThat(corsConfiguration.getAllowedHeaders()).containsExactly("Authorization", "Content-Type");
        assertThat(corsConfiguration.getAllowCredentials()).isFalse();
    }
}
