package com.zufar.icedlatte.order.internal;

import com.zufar.icedlatte.order.entity.OrderStatusHistory;
import com.zufar.icedlatte.order.event.OrderStatusChangedEvent;
import com.zufar.icedlatte.order.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.BEFORE_COMMIT;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusHistoryRecorder {

    private final OrderStatusHistoryRepository repository;

    @TransactionalEventListener(phase = BEFORE_COMMIT)
    public void onStatusChanged(OrderStatusChangedEvent event) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .orderId(event.orderId())
                .oldStatus(event.oldStatus())
                .newStatus(event.newStatus())
                .changedBy(event.changedBy())
                .reason(event.reason())
                .changedAt(event.timestamp())
                .build();

        repository.save(history);
        log.debug("order.status.history.recorded: orderId={}, {} → {}",
                event.orderId(), event.oldStatus(), event.newStatus());
    }
}
