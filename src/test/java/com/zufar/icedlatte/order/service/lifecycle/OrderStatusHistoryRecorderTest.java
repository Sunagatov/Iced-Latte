package com.zufar.icedlatte.order.service.lifecycle;

import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.entity.OrderStatusHistory;
import com.zufar.icedlatte.order.event.OrderStatusChangedEvent;
import com.zufar.icedlatte.order.repository.OrderStatusHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderStatusHistoryRecorder unit tests")
class OrderStatusHistoryRecorderTest {

    @Mock private OrderStatusHistoryRepository repository;
    @InjectMocks private OrderStatusHistoryRecorder recorder;

    @Test
    @DisplayName("Persists history entry from domain event")
    void onStatusChangedPersistsHistory() {
        UUID orderId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                orderId, OrderStatus.CREATED, OrderStatus.PAID, actorId, "Payment confirmed", now
        );

        recorder.onStatusChanged(event);

        ArgumentCaptor<OrderStatusHistory> captor = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(repository).save(captor.capture());
        OrderStatusHistory saved = captor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.getOldStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(saved.getNewStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(saved.getChangedBy()).isEqualTo(actorId);
        assertThat(saved.getReason()).isEqualTo("Payment confirmed");
        assertThat(saved.getChangedAt()).isEqualTo(now);
    }
}
