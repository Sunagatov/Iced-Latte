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
public class OrderExpirationJob {

    private final OrderRepository orderRepository;

    @Scheduled(fixedDelayString = "${order.expiration-check-interval-ms:3600000}")
    @Transactional
    public void expireUnpaidOrders() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusHours(24);

        Specification<Order> spec = Specification
                .where(OrderSpecifications.hasStatusIn(List.of(OrderStatus.CREATED)))
                .and((root, _, cb) -> cb.lessThan(root.get("createdAt"), cutoff));

        List<Order> expired = orderRepository.findAll(spec);

        for (Order order : expired) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("order.expired: orderId={}, createdAt={}", order.getId(), order.getCreatedAt());
        }

        if (!expired.isEmpty()) {
            log.info("order.expiration.completed: count={}", expired.size());
        }
    }
}
