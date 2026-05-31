package com.zufar.icedlatte.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PaginationConfig unit tests")
class PaginationConfigTest {

    @Test
    @DisplayName("exposes configured pagination values")
    void exposesConfiguredValues() {
        var config = new PaginationConfig(
                0,
                new PaginationConfig.Products(50, "name", "desc"),
                new PaginationConfig.Reviews(10, "createdAt", "desc"),
                new PaginationConfig.Orders(10, 50, "createdAt", "desc"));

        assertThat(config.defaultPageNumber()).isZero();
        assertThat(config.products().defaultPageSize()).isEqualTo(50);
        assertThat(config.reviews().defaultSortAttribute()).isEqualTo("createdAt");
        assertThat(config.orders().maxPageSize()).isEqualTo(50);
    }
}
