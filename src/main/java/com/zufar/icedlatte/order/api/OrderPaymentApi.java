package com.zufar.icedlatte.order.api;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NonNull;

/**
 * Narrow contract exposed to the payment module. Payment can only: look up order snapshots and notify order about
 * payment outcomes.
 */
public interface OrderPaymentApi {

    OrderSnapshot getSnapshot(@NonNull UUID orderId);

    OrderSnapshot getSnapshotWithItems(@NonNull UUID orderId);

    Optional<OrderSnapshot> findByStripePaymentIntentId(@NonNull String paymentIntentId);

    void confirmPayment(@NonNull UUID orderId, @NonNull String reason);

    void expirePayment(@NonNull UUID orderId, @NonNull String reason);

    void failPayment(@NonNull UUID orderId, @NonNull String reason);

    void assignPaymentIntent(@NonNull UUID orderId, @NonNull String stripePaymentIntentId);

    void confirmRefund(@NonNull UUID orderId, @NonNull String reason);
}
