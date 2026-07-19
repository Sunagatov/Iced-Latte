package com.zufar.icedlatte.order.entity;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Order entity")
class OrderTest {

    @Test
    @DisplayName("prePersist tolerates missing items")
    void prePersistToleratesMissingItems() {
        Order order = Order.builder().items(null).build();

        assertThatCode(order::prePersist).doesNotThrowAnyException();
    }
}
