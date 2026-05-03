package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.OrderEvent;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.event.OrderStatusChangedEvent;
import com.zufar.icedlatte.order.exception.OrderNotFoundException;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.order.validator.OrderStatusTransitionValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
    @Mock private OrderStateMachine stateMachine;
    @Mock private OrderStatusTransitionValidator validator;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private OrderStatusTransitioner transitioner;

    @Test
    @DisplayName("Transitions order status and publishes event")
    void transitionUpdatesStatusAndPublishesEvent() {
        UUID orderId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).userId(actorId).status(OrderStatus.CREATED).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(stateMachine.transition(OrderStatus.CREATED, OrderEvent.PAYMENT_CONFIRMED)).thenReturn(OrderStatus.PAID);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = transitioner.transition(orderId, OrderEvent.PAYMENT_CONFIRMED, actorId, "Stripe confirmed");

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(validator).validate(order, OrderEvent.PAYMENT_CONFIRMED, actorId);

        ArgumentCaptor<OrderStatusChangedEvent> eventCaptor = ArgumentCaptor.forClass(OrderStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        OrderStatusChangedEvent event = eventCaptor.getValue();
        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.oldStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(event.newStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(event.reason()).isEqualTo("Stripe confirmed");
    }

    @Test
    @DisplayName("Throws OrderNotFoundException when order does not exist")
    void transitionThrowsWhenOrderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transitioner.transition(orderId, OrderEvent.CANCEL, UUID.randomUUID()))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
