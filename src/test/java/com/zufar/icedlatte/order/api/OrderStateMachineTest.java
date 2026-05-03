package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.OrderEvent;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.exception.InvalidOrderStateTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrderStateMachine unit tests")
class OrderStateMachineTest {

    private final OrderStateMachine stateMachine = new OrderStateMachine();

    @ParameterizedTest(name = "{0} + {1} → {2}")
    @CsvSource({
            "CREATED, PAYMENT_CONFIRMED, PAID",
            "CREATED, CANCEL, CANCELLED",
            "PAID, SHIP, SHIPPED",
            "PAID, CANCEL, CANCELLED",
            "PAID, REQUEST_REFUND, REFUND_REQUESTED",
            "SHIPPED, DELIVER, DELIVERED",
            "REFUND_REQUESTED, REFUND_CONFIRMED, REFUNDED"
    })
    @DisplayName("Valid transitions produce correct target status")
    void validTransitions(OrderStatus from, OrderEvent event, OrderStatus expected) {
        assertThat(stateMachine.transition(from, event)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} + {1} → rejected")
    @CsvSource({
            "CREATED, SHIP",
            "CREATED, DELIVER",
            "CREATED, REQUEST_REFUND",
            "CREATED, REFUND_CONFIRMED",
            "PAID, PAYMENT_CONFIRMED",
            "PAID, DELIVER",
            "PAID, REFUND_CONFIRMED",
            "SHIPPED, PAYMENT_CONFIRMED",
            "SHIPPED, SHIP",
            "SHIPPED, CANCEL",
            "SHIPPED, REQUEST_REFUND",
            "DELIVERED, PAYMENT_CONFIRMED",
            "DELIVERED, CANCEL",
            "CANCELLED, PAYMENT_CONFIRMED",
            "CANCELLED, CANCEL",
            "REFUND_REQUESTED, CANCEL",
            "REFUNDED, CANCEL"
    })
    @DisplayName("Invalid transitions throw InvalidOrderStateTransitionException")
    void invalidTransitions(OrderStatus from, OrderEvent event) {
        assertThatThrownBy(() -> stateMachine.transition(from, event))
                .isInstanceOf(InvalidOrderStateTransitionException.class)
                .hasMessageContaining(event.toString())
                .hasMessageContaining(from.toString());
    }

    @Test
    @DisplayName("Terminal states have no outgoing transitions")
    void terminalStatesHaveNoTransitions() {
        for (OrderEvent event : OrderEvent.values()) {
            assertThatThrownBy(() -> stateMachine.transition(OrderStatus.DELIVERED, event))
                    .isInstanceOf(InvalidOrderStateTransitionException.class);
            assertThatThrownBy(() -> stateMachine.transition(OrderStatus.REFUNDED, event))
                    .isInstanceOf(InvalidOrderStateTransitionException.class);
        }
    }
}
