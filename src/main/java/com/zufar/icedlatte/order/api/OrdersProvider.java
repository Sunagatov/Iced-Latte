package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.OrderPageDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.openapi.dto.OrderSummaryDto;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.order.specification.OrderSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrdersProvider {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public OrderPageDto getOrders(UUID userId,
                                  List<OrderStatus> statuses,
                                  Integer year,
                                  LocalDate dateFrom,
                                  LocalDate dateTo,
                                  Pageable pageable) {
        Specification<Order> spec = Specification.where(OrderSpecifications.belongsToUser(userId));

        Specification<Order> statusSpec = OrderSpecifications.hasStatusIn(statuses);
        if (statusSpec != null) spec = spec.and(statusSpec);

        Specification<Order> yearSpec = OrderSpecifications.createdInYear(year);
        if (yearSpec != null) spec = spec.and(yearSpec);

        Specification<Order> fromSpec = OrderSpecifications.createdAfter(dateFrom);
        if (fromSpec != null) spec = spec.and(fromSpec);

        Specification<Order> toSpec = OrderSpecifications.createdBefore(dateTo);
        if (toSpec != null) spec = spec.and(toSpec);

        Page<Order> page = orderRepository.findAll(spec, pageable);

        List<OrderSummaryDto> content = page.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new OrderPageDto()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages());
    }

    private OrderSummaryDto toSummary(Order order) {
        String firstItemName = order.getItems() != null && !order.getItems().isEmpty()
                ? order.getItems().getFirst().getProductName()
                : null;

        return new OrderSummaryDto()
                .id(order.getId())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .itemsQuantity(order.getItemsQuantity())
                .itemsTotalPrice(order.getItemsTotalPrice() != null ? order.getItemsTotalPrice().doubleValue() : null)
                .firstItemName(firstItemName)
                .itemCount(order.getItems() != null ? order.getItems().size() : 0);
    }
}
