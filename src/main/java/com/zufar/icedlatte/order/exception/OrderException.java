package com.zufar.icedlatte.order.exception;

/**
 * Sealed base for all order-related exceptions.
 * Enables exhaustive pattern matching in switch expressions (Java 25).
 */
public abstract sealed class OrderException extends RuntimeException
        permits OrderNotFoundException, OrderAccessDeniedException,
                InvalidOrderStateTransitionException, OrderCancellationWindowExpiredException {

    protected OrderException(String message) {
        super(message);
    }
}
