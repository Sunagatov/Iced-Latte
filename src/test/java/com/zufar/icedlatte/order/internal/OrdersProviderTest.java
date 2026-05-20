package com.zufar.icedlatte.order.internal;

import com.zufar.icedlatte.openapi.dto.OrderPageDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.entity.OrderItem;
import com.zufar.icedlatte.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrdersProvider unit tests")
@SuppressWarnings("unchecked")
class OrdersProviderTest {

    @Mock
    private OrderRepository orderRepository;
    @InjectMocks
    private OrdersProvider ordersProvider;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Test
    @DisplayName("Returns paginated orders with summary DTOs")
    void getOrdersReturnsPaginatedResults() {
        Order order = buildOrder();
        when(orderRepository.findAll(any(Specification.class), eq(PAGEABLE)))
                .thenReturn(new PageImpl<>(List.of(order), PAGEABLE, 1));

        OrderPageDto result = ordersProvider.getOrders(USER_ID, null, null, null, null, PAGEABLE);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getFirstItemName()).isEqualTo("Nitro Cold Brew");
        assertThat(result.getContent().getFirst().getItemCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Returns empty page when no orders match")
    void getOrdersReturnsEmptyPage() {
        when(orderRepository.findAll(any(Specification.class), eq(PAGEABLE)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PAGEABLE, 0));

        OrderPageDto result = ordersProvider.getOrders(USER_ID, null, null, null, null, PAGEABLE);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Passes status filter through to specification")
    void getOrdersWithStatusFilter() {
        when(orderRepository.findAll(any(Specification.class), eq(PAGEABLE)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PAGEABLE, 0));

        OrderPageDto result = ordersProvider.getOrders(USER_ID, List.of(OrderStatus.PAID), null, null, null, PAGEABLE);

        assertThat(result.getTotalElements()).isZero();
    }

    private Order buildOrder() {
        OrderItem item = OrderItem.builder()
                .id(UUID.randomUUID())
                .productName("Nitro Cold Brew")
                .productPrice(BigDecimal.TEN)
                .productsQuantity(1)
                .build();
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .status(OrderStatus.CREATED)
                .items(List.of(item))
                .itemsQuantity(1)
                .itemsTotalPrice(BigDecimal.TEN)
                .build();
        order.setCreatedAt(OffsetDateTime.now());
        return order;
    }
}
