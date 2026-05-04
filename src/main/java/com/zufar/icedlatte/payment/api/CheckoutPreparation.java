package com.zufar.icedlatte.payment.api;

import com.zufar.icedlatte.openapi.dto.ShoppingCartItemDto;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.payment.entity.Payment;

import java.util.List;

/**
 * Result of {@link CheckoutPaymentTransactionService#prepareCheckout}.
 * If {@code existing} is true, this is an idempotent hit. Do not read the live cart.
 */
public record CheckoutPreparation(
        Order order,
        Payment payment,
        List<ShoppingCartItemDto> cartItems,
        boolean existing) {
}
