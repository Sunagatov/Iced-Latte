package com.zufar.onlinestore.reservation.api.dto.creation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * @param warehouseItemId Product item ID in warehouse for reservation
 * @param quantity        Quantity of product item for reservation
 */
public record ProductReservation(

        @NotNull
        UUID warehouseItemId,

        @NotNull
        @Min(value = MIN_RESERVATION_QUANTITY_FOR_PRODUCT_PER_USER)
        @Max(value = MAX_RESERVATION_QUANTITY_FOR_PRODUCT_PER_USER)
        Integer quantity
) {
    public static final int MIN_RESERVATION_QUANTITY_FOR_PRODUCT_PER_USER = 1;
    public static final int MAX_RESERVATION_QUANTITY_FOR_PRODUCT_PER_USER = 10;

    public static ProductReservation outOfStockProductReservation(UUID productId) {
        return new ProductReservation(productId, 0);
    }
}
