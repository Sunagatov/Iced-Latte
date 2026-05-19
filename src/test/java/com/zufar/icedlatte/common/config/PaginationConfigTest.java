package com.zufar.icedlatte.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaginationConfig unit tests")
class PaginationConfigTest {

    @Test
    @DisplayName("exposes the documented default pagination values")
    void exposesDefaultValues() {
        var config = new PaginationConfig(0,
                new PaginationConfig.Products(50, "name", "desc"),
                new PaginationConfig.Reviews(10, "createdAt", "desc"),
                new PaginationConfig.Orders(10, 50, "createdAt", "desc"));

        assertThat(config.defaultPageNumber()).isZero();
        assertThat(config.products().defaultPageSize()).isEqualTo(50);
        assertThat(config.products().defaultSortAttribute()).isEqualTo("name");
        assertThat(config.products().defaultSortDirection()).isEqualTo("desc");
        assertThat(config.reviews().defaultPageSize()).isEqualTo(10);
        assertThat(config.reviews().defaultSortAttribute()).isEqualTo("createdAt");
        assertThat(config.reviews().defaultSortDirection()).isEqualTo("desc");
    }

    @Test
    @DisplayName("supports custom pagination values")
    void supportsCustomValues() {
        var config = new PaginationConfig(2,
                new PaginationConfig.Products(24, "price", "asc"),
                new PaginationConfig.Reviews(5, "likesCount", "asc"),
                new PaginationConfig.Orders(20, 100, "updatedAt", "asc"));

        assertThat(config.defaultPageNumber()).isEqualTo(2);
        assertThat(config.products().defaultPageSize()).isEqualTo(24);
        assertThat(config.products().defaultSortAttribute()).isEqualTo("price");
        assertThat(config.products().defaultSortDirection()).isEqualTo("asc");
        assertThat(config.reviews().defaultPageSize()).isEqualTo(5);
        assertThat(config.reviews().defaultSortAttribute()).isEqualTo("likesCount");
        assertThat(config.reviews().defaultSortDirection()).isEqualTo("asc");
    }
}
