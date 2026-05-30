package com.zufar.icedlatte.product.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProductInfo entity tests")
class ProductInfoTest {

    @Test
    @DisplayName("transient products without ids are not equal")
    void transientProductsWithoutIdsAreNotEqual() {
        assertThat(new ProductInfo()).isNotEqualTo(new ProductInfo());
    }

    @Test
    @DisplayName("persisted products with the same id are equal")
    void persistedProductsWithSameIdAreEqual() {
        UUID id = UUID.randomUUID();
        ProductInfo first = new ProductInfo();
        first.setId(id);
        ProductInfo second = new ProductInfo();
        second.setId(id);

        assertThat(first).isEqualTo(second);
    }
}
