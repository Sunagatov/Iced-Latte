package com.zufar.icedlatte.payment.dto;

import java.util.List;

import com.zufar.icedlatte.cart.api.dto.CartItemSnapshot;
import com.zufar.icedlatte.order.api.OrderSnapshot;
import com.zufar.icedlatte.payment.service.checkout.CheckoutPaymentTransactionService;

/**
 * Result of {@link CheckoutPaymentTransactionService#prepareCheckout}. If {@code existing} is true, this is an
 * idempotent hit. Do not read the live cart.
 */
public record CheckoutPreparation(
        OrderSnapshot order, CheckoutPaymentSnapshot payment, List<CartItemSnapshot> cartItems, boolean existing) {}
