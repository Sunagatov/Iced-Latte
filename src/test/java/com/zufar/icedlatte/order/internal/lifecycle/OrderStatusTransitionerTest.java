package com.zufar.icedlatte.order.internal.lifecycle;

import com.zufar.icedlatte.openapi.dto.OrderEvent;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.event.OrderStatusChangedEvent;
import com.zufar.icedlatte.order.exception.InvalidOrderStateTransitionException;
import com.zufar.icedlatte.order.exception.OrderCancellationWindowExpiredException;
import com.zufar.icedlatte.order.exception.OrderNotFoundException;
import com.zufar.icedlatte.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderStatusTransitioner unit tests")
class OrderStatusTransitionerTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private OrderStatusTransitioner transitioner;

    @ParameterizedTest(name = "{0} + {1} → {2}")
    @CsvSource({
            "CREATED, PAYMENT_CONFIRMED, PAID",
            "CREATED, CANCEL, CANCELLED",
            "PAID, SHIP, SHIPPED",
            "PAID, CANCEL, CANCELLED",
            "PAID, REQUEST_REFUND, REFUND_REQUESTED",
            "SHIPPED, DELIVER, DELIVERED",
            "REFUND_REQUESTED, REFUND_CONFIRMED, REFUNDED",
            "PENDING_PAYMENT, PENDING_PAYMENT_CONFIRMED, PAID",
            "PENDING_PAYMENT, PAYMENT_FAILED_EVENT, PAYMENT_FAILED",
            "PENDING_PAYMENT, PAYMENT_EXPIRED_EVENT, PAYMENT_EXPIRED",
            "PENDING_PAYMENT, CANCEL, CANCELLED"
    })
    @DisplayName("Valid transitions produce correct target status")
    void validTransitions(OrderStatus from, OrderEvent event, OrderStatus expected) {
        UUID orderId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).userId(actorId).status(from)
                .cancellationDeadline(OffsetDateTime.now().plusHours(1)).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order result = transitioner.transition(orderId, event, actorId);

        assertThat(result.getStatus()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} + {1} → rejected")
    @CsvSource({
            "CREATED, SHIP",
            "CREATED, DELIVER",
            "SHIPPED, CANCEL",
            "DELIVERED, CANCEL",
            "CANCELLED, CANCEL",
            "REFUNDED, CANCEL",
            "PENDING_PAYMENT, SHIP",
            "PENDING_PAYMENT, DELIVER",
            "PENDING_PAYMENT, PAYMENT_CONFIRMED",
            "PAYMENT_FAILED, CANCEL",
            "PAYMENT_EXPIRED, CANCEL"
    })
    @DisplayName("Invalid transitions throw InvalidOrderStateTransitionException")
    void invalidTransitions(OrderStatus from, OrderEvent event) {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).userId(UUID.randomUUID()).status(from)
                .cancellationDeadline(OffsetDateTime.now().plusHours(1)).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> transitioner.transition(orderId, event, UUID.randomUUID()))
                .isInstanceOf(InvalidOrderStateTransitionException.class);
    }

    @Test
    @DisplayName("Publishes OrderStatusChangedEvent on successful transition")
    void publishesEvent() {
        UUID orderId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).userId(actorId).status(OrderStatus.CREATED)
                .cancellationDeadline(OffsetDateTime.now().plusHours(1)).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        transitioner.transition(orderId, OrderEvent.PAYMENT_CONFIRMED, actorId, "Stripe confirmed");

        ArgumentCaptor<OrderStatusChangedEvent> captor = ArgumentCaptor.forClass(OrderStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().oldStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(captor.getValue().newStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(captor.getValue().reason()).isEqualTo("Stripe confirmed");
    }

    @Test
    @DisplayName("Throws OrderNotFoundException when order does not exist")
    void orderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transitioner.transition(orderId, OrderEvent.CANCEL, UUID.randomUUID()))
                .isInstanceOf(OrderNotFoundException.class);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Cancel after deadline throws OrderCancellationWindowExpiredException")
    void cancelAfterDeadline() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).userId(UUID.randomUUID()).status(OrderStatus.CREATED)
                .cancellationDeadline(OffsetDateTime.now().minusHours(1)).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> transitioner.transition(orderId, OrderEvent.CANCEL, UUID.randomUUID()))
                .isInstanceOf(OrderCancellationWindowExpiredException.class);
    }
}
