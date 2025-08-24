package com.zufar.icedlatte.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = PaginationConfig.class)
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
class PaginationConfigTest {

    @Autowired
    private PaginationConfig paginationConfig;

    @Test
    void shouldLoadDefaultPaginationConfiguration() {
        assertEquals(0, paginationConfig.getDefaultPageNumber());
    }

    @Test
    void shouldLoadProductsPaginationConfiguration() {
        assertEquals(50, paginationConfig.getProducts().getDefaultPageSize());
        assertEquals("name", paginationConfig.getProducts().getDefaultSortAttribute());
        assertEquals("desc", paginationConfig.getProducts().getDefaultSortDirection());
    }

    @Test
    void shouldLoadReviewsPaginationConfiguration() {
        assertEquals(10, paginationConfig.getReviews().getDefaultPageSize());
        assertEquals("createdAt", paginationConfig.getReviews().getDefaultSortAttribute());
        assertEquals("desc", paginationConfig.getReviews().getDefaultSortDirection());
    }
}