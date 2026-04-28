package com.zufar.icedlatte.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CacheProperties unit tests")
class CachePropertiesTest {

    @Test
    @DisplayName("exposes documented default TTL values")
    void exposesDefaultTtls() {
        CacheProperties properties = new CacheProperties();

        assertThat(properties.getDefaultTtl()).isEqualTo(Duration.ofMinutes(10));
        assertThat(properties.getProductTtl()).isEqualTo(Duration.ofMinutes(10));
        assertThat(properties.getBrandsTtl()).isEqualTo(Duration.ofHours(24));
        assertThat(properties.getSellersTtl()).isEqualTo(Duration.ofHours(24));
        assertThat(properties.getImageUrlTtl()).isEqualTo(Duration.ofMinutes(50));
        assertThat(properties.getImageUrlsTtl()).isEqualTo(Duration.ofHours(24));
    }

    @Test
    @DisplayName("allows overriding each TTL independently")
    void allowsOverridingTtls() {
        CacheProperties properties = new CacheProperties();

        properties.setDefaultTtl(Duration.ofSeconds(30));
        properties.setProductTtl(Duration.ofMinutes(2));
        properties.setBrandsTtl(Duration.ofHours(12));
        properties.setSellersTtl(Duration.ofHours(6));
        properties.setImageUrlTtl(Duration.ofMinutes(5));
        properties.setImageUrlsTtl(Duration.ofMinutes(45));

        assertThat(properties.getDefaultTtl()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.getProductTtl()).isEqualTo(Duration.ofMinutes(2));
        assertThat(properties.getBrandsTtl()).isEqualTo(Duration.ofHours(12));
        assertThat(properties.getSellersTtl()).isEqualTo(Duration.ofHours(6));
        assertThat(properties.getImageUrlTtl()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.getImageUrlsTtl()).isEqualTo(Duration.ofMinutes(45));
    }
}
