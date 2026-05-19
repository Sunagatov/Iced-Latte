package com.zufar.icedlatte.common.exception;

/**
 * Centralized RFC 9457 problem-detail type slugs.
 * Each slug is appended to the configured problem.type-base-url.
 */
public final class ProblemType {

    private ProblemType() {}

    // Auth & Security
    public static final String AUTH_REQUIRED = "auth-required";
    public static final String ACCESS_DENIED = "access-denied";
    public static final String INVALID_CREDENTIALS = "invalid-credentials";
    public static final String SESSION_EXPIRED = "session-expired";
    public static final String AUTH_FAILED = "auth-failed";
    public static final String REGISTRATION_FAILED = "registration-failed";
    public static final String ACCOUNT_LOCKED = "account-locked";
    public static final String SESSION_NOT_FOUND = "session-not-found";
    public static final String SESSION_ACCESS_DENIED = "session-access-denied";
    public static final String INTERNAL_ERROR = "internal-error";
    public static final String RATE_LIMITED = "rate-limited";
    public static final String TURNSTILE_FAILED = "turnstile-failed";

    // Orders
    public static final String ORDER_NOT_FOUND = "order-not-found";
    public static final String ORDER_ACCESS_DENIED = "order-access-denied";
    public static final String ORDER_STATE_INVALID = "order-state-invalid";
    public static final String ORDER_CANCELLATION_EXPIRED = "order-cancellation-expired";
    public static final String INVALID_PARAMETER = "invalid-parameter";

    // Cart
    public static final String CART_NOT_FOUND = "cart-not-found";
    public static final String CART_ITEM_NOT_FOUND = "cart-item-not-found";
    public static final String CART_INVALID_QUANTITY = "cart-invalid-quantity";

    // Products & Reviews

    // Validation & Files
    public static final String VALIDATION_FAILED = "validation-failed";
    public static final String FILE_TOO_LARGE = "file-too-large";
    public static final String FILE_READ_FAILED = "file-read-failed";
    public static final String FILE_UPLOAD_FAILED = "file-upload-failed";

    // Payment
    public static final String PAYMENT_EVENT_FAILED = "payment-event-failed";
    public static final String PAYMENT_SESSION_FAILED = "payment-session-failed";
}
