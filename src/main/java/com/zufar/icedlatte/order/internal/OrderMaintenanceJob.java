package com.zufar.icedlatte.order.internal;

import com.zufar.icedlatte.common.monitoring.SentryJobMonitor;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.order.specification.OrderSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "order.maintenance.enabled", havingValue = "true", matchIfMissing = true)
public class OrderMaintenanceJob {

    private static final String EXPIRATION_MONITOR_SLUG = "order-expiration-job";
    private static final String REFUND_MONITOR_SLUG = "order-refund-monitor-job";

    private final OrderRepository orderRepository;
    private final SentryJobMonitor sentryJobMonitor;
    private final PlatformTransactionManager transactionManager;

    @Value("${order.expiration-check-interval-ms:3600000}")
    private long expirationCheckIntervalMs;

    @Value("${order.refund-monitor-interval-ms:3600000}")
    private long refundMonitorIntervalMs;

    @Scheduled(fixedDelayString = "${order.expiration-check-interval-ms:3600000}")
    public void expireUnpaidOrders() {
        sentryJobMonitor.run(EXPIRATION_MONITOR_SLUG, sentryJobMonitor.fixedDelayConfig(expirationCheckIntervalMs),
                () -> transactionTemplate(false).executeWithoutResult(_ -> expireUnpaidOrdersInternal()));
    }

    private void expireUnpaidOrdersInternal() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusHours(24);
        Specification<Order> spec = Specification
                .where(OrderSpecifications.hasStatusIn(List.of(OrderStatus.CREATED)))
                .and((root, _, cb) -> cb.lessThan(root.get("createdAt"), cutoff));

        List<Order> expired = orderRepository.findAll(spec);
        for (Order order : expired) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("order.expired: orderId={}", order.getId());
        }
        if (!expired.isEmpty()) {
            log.info("order.expiration.completed: count={}", expired.size());
        }
    }

    @Scheduled(fixedDelayString = "${order.refund-monitor-interval-ms:3600000}")
    public void checkStuckRefunds() {
        sentryJobMonitor.run(REFUND_MONITOR_SLUG, sentryJobMonitor.fixedDelayConfig(refundMonitorIntervalMs),
                () -> transactionTemplate(true).executeWithoutResult(_ -> checkStuckRefundsInternal()));
    }

    private void checkStuckRefundsInternal() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusHours(4);
        Specification<Order> spec = Specification
                .where(OrderSpecifications.hasStatusIn(List.of(OrderStatus.REFUND_REQUESTED)))
                .and((root, _, cb) -> cb.lessThan(root.get("updatedAt"), cutoff));

        List<Order> stuck = orderRepository.findAll(spec);
        for (Order order : stuck) {
            log.warn("order.refund.stuck: orderId={}, stripePaymentIntentId={}",
                    order.getId(), order.getStripePaymentIntentId());
        }
        if (!stuck.isEmpty()) {
            log.warn("order.refund.stuck.total: count={}", stuck.size());
        }
    }

    private TransactionTemplate transactionTemplate(boolean readOnly) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setReadOnly(readOnly);
        return transactionTemplate;
    }
}
