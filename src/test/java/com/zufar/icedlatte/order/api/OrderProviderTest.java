package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderProvider unit tests")
class OrderProviderTest {

    @Mock
    private OrderRepository orderRepository;
    @InjectMocks
    private OrderProvider orderProvider;

    @Test
    @DisplayName("Returns order when found by userId and sessionId")
    void getOrderEntityByUserAndSession_found_returnsOrder() {
        UUID userId = UUID.randomUUID();
        String sessionId = "session-abc";
        Order order = Order.builder().id(UUID.randomUUID()).userId(userId).sessionId(sessionId).build();
        when(orderRepository.findByUserIdAndSessionId(userId, sessionId)).thenReturn(Optional.of(order));

        Optional<Order> result = orderProvider.getOrderEntityByUserAndSession(userId, sessionId);

        assertThat(result).isPresent().contains(order);
    }

    @Test
    @DisplayName("Returns empty Optional when no order found")
    void getOrderEntityByUserAndSession_notFound_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        String sessionId = "missing-session";
        when(orderRepository.findByUserIdAndSessionId(userId, sessionId)).thenReturn(Optional.empty());

        Optional<Order> result = orderProvider.getOrderEntityByUserAndSession(userId, sessionId);

        assertThat(result).isEmpty();
    }
}
