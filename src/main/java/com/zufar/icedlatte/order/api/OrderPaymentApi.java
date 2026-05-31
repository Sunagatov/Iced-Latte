package com.zufar.icedlatte.order.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Narrow contract exposed to the payment module. Payment can only: look up order snapshots and notify order about
 * payment outcomes.
 */
public interface OrderPaymentApi {

    OrderSnapshot getSnapshot(UUID orderId);

    OrderSnapshot getSnapshotWithItems(UUID orderId);

    Optional<OrderSnapshot> findByStripePaymentIntentId(String paymentIntentId);

    boolean confirmPayment(UUID orderId, String reason);

    boolean expirePayment(UUID orderId, String reason);

    boolean failPayment(UUID orderId, String reason);

    void assignPaymentIntent(UUID orderId, String stripePaymentIntentId);

    boolean confirmRefund(UUID orderId, String reason);
}
