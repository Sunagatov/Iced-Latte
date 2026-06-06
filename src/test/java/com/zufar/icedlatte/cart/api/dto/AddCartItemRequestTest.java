package com.zufar.icedlatte.cart.api.dto;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AddCartItemRequest")
class AddCartItemRequestTest {

    @Test
    @DisplayName("rejects missing product id")
    void rejectsMissingProductId() {
        assertThatThrownBy(() -> new AddCartItemRequest(null, 1)).isInstanceOf(NullPointerException.class);
    }
}
