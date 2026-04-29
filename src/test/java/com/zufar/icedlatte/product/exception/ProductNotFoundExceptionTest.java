package com.zufar.icedlatte.product.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductNotFoundException")
class ProductNotFoundExceptionTest {

    @Test
    @DisplayName("single-id constructor keeps the missing id")
    void singleIdConstructorKeepsMissingId() {
        UUID productId = UUID.randomUUID();

        ProductNotFoundException exception = new ProductNotFoundException(productId);

        assertThat(exception.getProductIds()).containsExactly(productId);
        assertThat(exception).hasMessage("The product with productId = " + productId + " is not found.");
    }

    @Test
    @DisplayName("list constructor renders all missing ids")
    void listConstructorRendersAllMissingIds() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        ProductNotFoundException exception = new ProductNotFoundException(List.of(first, second));

        assertThat(exception.getProductIds()).containsExactly(first, second);
        assertThat(exception.getMessage()).contains(first.toString()).contains(second.toString());
    }
}
