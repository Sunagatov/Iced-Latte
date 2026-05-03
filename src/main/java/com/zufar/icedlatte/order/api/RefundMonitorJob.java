package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.order.specification.OrderSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundMonitorJob {

    private final OrderRepository orderRepository;

    @Scheduled(fixedDelayString = "${order.refund-monitor-interval-ms:3600000}")
    @Transactional(readOnly = true)
    public void checkStuckRefunds() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusHours(4);

        Specification<Order> spec = Specification
                .where(OrderSpecifications.hasStatusIn(List.of(OrderStatus.REFUND_REQUESTED)))
                .and((root, _, cb) -> cb.lessThan(root.get("updatedAt"), cutoff));

        List<Order> stuck = orderRepository.findAll(spec);

        for (Order order : stuck) {
            log.warn("order.refund.stuck: orderId={}, stripePaymentIntentId={}, updatedAt={}",
                    order.getId(), order.getStripePaymentIntentId(), order.getUpdatedAt());
        }

        if (!stuck.isEmpty()) {
            log.warn("order.refund.stuck.total: count={}", stuck.size());
        }
    }
}
