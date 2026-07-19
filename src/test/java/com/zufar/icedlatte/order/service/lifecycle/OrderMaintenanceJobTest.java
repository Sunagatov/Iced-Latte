package com.zufar.icedlatte.order.service.lifecycle;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import com.zufar.icedlatte.common.monitoring.SentryJobMonitor;
import com.zufar.icedlatte.openapi.dto.OrderEvent;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.exception.InvalidOrderStateTransitionException;
import com.zufar.icedlatte.order.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderMaintenanceJob")
class OrderMaintenanceJobTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusTransitioner orderStatusTransitioner;

    @Mock
    @SuppressWarnings("unused")
    private SentryJobMonitor sentryJobMonitor;

    @Mock
    @SuppressWarnings("unused")
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private OrderMaintenanceJob orderMaintenanceJob;

    @Test
    @DisplayName("expires unpaid orders through lifecycle transitioner")
    void expireUnpaidOrdersInternalUsesTransitioner() {
        UUID orderId = UUID.randomUUID();
        ReflectionTestUtils.setField(orderMaintenanceJob, "batchSize", 100);
        when(orderRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Order>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(Order.builder().id(orderId).build())));

        orderMaintenanceJob.expireUnpaidOrdersInternal();

        verify(orderStatusTransitioner).expireUnpaid(orderId, "Unpaid order expired");
        verify(orderRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("continues expiring remaining orders when one stale order cannot transition")
    void expireUnpaidOrdersInternalSkipsStaleOrderAndContinues() {
        UUID staleOrderId = UUID.randomUUID();
        UUID nextOrderId = UUID.randomUUID();
        ReflectionTestUtils.setField(orderMaintenanceJob, "batchSize", 100);
        when(orderRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Order>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        Order.builder().id(staleOrderId).build(),
                        Order.builder().id(nextOrderId).build())));
        when(orderStatusTransitioner.expireUnpaid(staleOrderId, "Unpaid order expired"))
                .thenThrow(new InvalidOrderStateTransitionException(OrderStatus.PAID, OrderEvent.CANCEL));

        orderMaintenanceJob.expireUnpaidOrdersInternal();

        verify(orderStatusTransitioner).expireUnpaid(staleOrderId, "Unpaid order expired");
        verify(orderStatusTransitioner).expireUnpaid(nextOrderId, "Unpaid order expired");
    }
}
