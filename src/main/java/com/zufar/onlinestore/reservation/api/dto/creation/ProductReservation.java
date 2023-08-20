package com.zufar.onlinestore.reservation.api.dto.creation;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * @param warehouseItemId Product item ID in warehouse
 * @param quantity        Quantity of product
 */
public record ProductReservation(

        @NotNull
        UUID warehouseItemId,

        @NotNull
        Integer quantity
) {
    public static final int MIN_RESERVATION_QUANTITY_FOR_PRODUCT_PER_USER = 1;
    public static final int MAX_RESERVATION_QUANTITY_FOR_PRODUCT_PER_USER = 10;

    public static ProductReservation outOfStockProductReservation(UUID productId) {
        return new ProductReservation(productId, 0);
    }
}
