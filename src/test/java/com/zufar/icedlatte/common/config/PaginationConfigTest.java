package com.zufar.icedlatte.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PaginationConfig.class)
@EnableConfigurationProperties(PaginationConfig.class)
@TestPropertySource(properties = {
    "pagination.defaultPageNumber=0",
    "pagination.products.defaultPageSize=50",
    "pagination.products.defaultSortAttribute=name",
    "pagination.products.defaultSortDirection=desc",
    "pagination.reviews.defaultPageSize=10",
    "pagination.reviews.defaultSortAttribute=createdAt",
    "pagination.reviews.defaultSortDirection=desc"
})
@DisplayName("PaginationConfig Tests")
class PaginationConfigTest {

    @Autowired
    private PaginationConfig paginationConfig;

    @Test
    @DisplayName("Should load default pagination configuration")
    void shouldLoadDefaultPaginationConfiguration() {
        assertEquals(0, paginationConfig.getDefaultPageNumber());
    }

    @Test
    @DisplayName("Should load products pagination configuration")
    void shouldLoadProductsPaginationConfiguration() {
        assertEquals(50, paginationConfig.getProducts().getDefaultPageSize());
        assertEquals("name", paginationConfig.getProducts().getDefaultSortAttribute());
        assertEquals("desc", paginationConfig.getProducts().getDefaultSortDirection());
    }

    @Test
    @DisplayName("Should load reviews pagination configuration")
    void shouldLoadReviewsPaginationConfiguration() {
        assertEquals(10, paginationConfig.getReviews().getDefaultPageSize());
        assertEquals("createdAt", paginationConfig.getReviews().getDefaultSortAttribute());
        assertEquals("desc", paginationConfig.getReviews().getDefaultSortDirection());
    }
}