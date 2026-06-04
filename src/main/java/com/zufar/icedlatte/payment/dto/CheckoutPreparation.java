package com.zufar.icedlatte.payment.dto;

import java.util.List;

import com.zufar.icedlatte.cart.api.dto.CartItemSnapshot;
import com.zufar.icedlatte.order.api.OrderSnapshot;
import com.zufar.icedlatte.payment.service.checkout.CheckoutPaymentTransactionService;

/**
 * Result of {@link CheckoutPaymentTransactionService#prepareCheckout}. Existing checkout retries use the persisted
 * order snapshot and never read the live cart.
 */
public sealed interface CheckoutPreparation
        permits CheckoutPreparation.NewCheckout, CheckoutPreparation.ExistingCheckout {

    OrderSnapshot order();

    CheckoutPaymentSnapshot payment();

    record NewCheckout(OrderSnapshot order, CheckoutPaymentSnapshot payment, List<CartItemSnapshot> cartItems)
            implements CheckoutPreparation {}

    record ExistingCheckout(OrderSnapshot order, CheckoutPaymentSnapshot payment) implements CheckoutPreparation {}
}
