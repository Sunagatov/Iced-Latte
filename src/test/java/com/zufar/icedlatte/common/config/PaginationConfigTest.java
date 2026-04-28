package com.zufar.icedlatte.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaginationConfig unit tests")
class PaginationConfigTest {

    @Test
    @DisplayName("exposes the documented default pagination values")
    void exposesDefaultValues() {
        PaginationConfig config = new PaginationConfig();

        assertThat(config.getDefaultPageNumber()).isZero();
        assertThat(config.getProducts().getDefaultPageSize()).isEqualTo(50);
        assertThat(config.getProducts().getDefaultSortAttribute()).isEqualTo("name");
        assertThat(config.getProducts().getDefaultSortDirection()).isEqualTo("desc");
        assertThat(config.getReviews().getDefaultPageSize()).isEqualTo(10);
        assertThat(config.getReviews().getDefaultSortAttribute()).isEqualTo("createdAt");
        assertThat(config.getReviews().getDefaultSortDirection()).isEqualTo("desc");
    }

    @Test
    @DisplayName("allows overriding nested product and review defaults")
    void allowsOverridingNestedDefaults() {
        PaginationConfig config = new PaginationConfig();

        config.setDefaultPageNumber(2);
        config.getProducts().setDefaultPageSize(24);
        config.getProducts().setDefaultSortAttribute("price");
        config.getProducts().setDefaultSortDirection("asc");
        config.getReviews().setDefaultPageSize(5);
        config.getReviews().setDefaultSortAttribute("likesCount");
        config.getReviews().setDefaultSortDirection("asc");

        assertThat(config.getDefaultPageNumber()).isEqualTo(2);
        assertThat(config.getProducts().getDefaultPageSize()).isEqualTo(24);
        assertThat(config.getProducts().getDefaultSortAttribute()).isEqualTo("price");
        assertThat(config.getProducts().getDefaultSortDirection()).isEqualTo("asc");
        assertThat(config.getReviews().getDefaultPageSize()).isEqualTo(5);
        assertThat(config.getReviews().getDefaultSortAttribute()).isEqualTo("likesCount");
        assertThat(config.getReviews().getDefaultSortDirection()).isEqualTo("asc");
    }
}
