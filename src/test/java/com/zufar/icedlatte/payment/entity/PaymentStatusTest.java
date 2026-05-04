package com.zufar.icedlatte.payment.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentStatus unit tests")
class PaymentStatusTest {

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"PAID", "REFUNDED", "RECONCILIATION_FAILED", "FAILED", "EXPIRED"})
    @DisplayName("Terminal statuses return true")
    void isTerminal_terminalStatuses_returnsTrue(PaymentStatus status) {
        assertThat(status.isTerminal()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"CREATED", "STRIPE_SESSION_CREATED", "AWAITING_ASYNC_CONFIRMATION"})
    @DisplayName("Non-terminal statuses return false")
    void isTerminal_nonTerminalStatuses_returnsFalse(PaymentStatus status) {
        assertThat(status.isTerminal()).isFalse();
    }
}
