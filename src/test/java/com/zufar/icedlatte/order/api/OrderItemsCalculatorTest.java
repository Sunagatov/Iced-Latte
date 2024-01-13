package com.zufar.icedlatte.order.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.zufar.icedlatte.order.api.OrderItemsCalculator.calculate;
import static com.zufar.icedlatte.order.stub.OrderDtoTestStub.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("OrderItemsCalculator Tests")
class OrderItemsCalculatorTest {

    @Test
    @DisplayName("toItemsTotalPrice should calculate items total price")
    void shouldCalculateItemsTotalPrice() {
        var items = List.of(createFirstOrderItem(null), createSecondOrderItem(null));
        var result = calculate(items);
        assertEquals(EXPECTED_ITEMS_TOTAL_PRICE, result);
    }

    @Test
    @DisplayName("toTotalOrderCost should calculate order total cost")
    void shouldCalculateOrderTotalCost() {
        var result = calculate(EXPECTED_ITEMS_TOTAL_PRICE, DELIVERY_COST, TAX_COST);
        assertEquals(EXPECTED_ORDER_TOTAL_COST, result);
    }

    @Test
    @DisplayName("toTotalItems should return the total amount of product items")
    void shouldCalculateTotalItems() {
        var items = List.of(createFirstOrderItem(null), createSecondOrderItem(null));
        var result = calculate(items, 0);
        assertEquals(EXPECTED_ITEMS_QUANTITY, result);
    }
}
