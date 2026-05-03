package com.zufar.icedlatte.order.endpoint;

import com.zufar.icedlatte.common.config.PaginationConfig;
import com.zufar.icedlatte.common.pagination.PageRequestFactory;
import com.zufar.icedlatte.openapi.dto.AdminOrderStatusUpdateDto;
import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderPageDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.api.OrderStatusTransitioner;
import com.zufar.icedlatte.order.api.OrdersProvider;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(AdminOrderEndpoint.ADMIN_ORDERS_URL)
public class AdminOrderEndpoint {

    public static final String ADMIN_ORDERS_URL = "/api/v1/admin/orders";

    private final OrdersProvider ordersProvider;
    private final OrderStatusTransitioner statusTransitioner;
    private final OrderDtoConverter orderDtoConverter;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final PaginationConfig paginationConfig;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<OrderPageDto> getAllOrders(
            @RequestParam(required = false) final List<OrderStatus> status,
            @RequestParam(required = false) final UUID userId,
            @RequestParam(required = false) final Integer page,
            @RequestParam(required = false) final Integer size,
            @RequestParam(required = false) final String sortBy,
            @RequestParam(required = false) final String sortDirection,
            @RequestParam(required = false) final Integer year,
            @RequestParam(required = false) final LocalDate dateFrom,
            @RequestParam(required = false) final LocalDate dateTo) {
        var defaults = paginationConfig.getOrders();
        Pageable pageable = PageRequestFactory.of(
                page != null ? page : paginationConfig.getDefaultPageNumber(),
                size != null ? Math.min(size, defaults.getMaxPageSize()) : defaults.getDefaultPageSize(),
                sortBy != null ? sortBy : defaults.getDefaultSortAttribute(),
                sortDirection != null ? sortDirection : defaults.getDefaultSortDirection()
        );
        var result = ordersProvider.getOrders(userId, status, year, dateFrom, dateTo, pageable);
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @PathVariable final UUID orderId,
            @Valid @RequestBody final AdminOrderStatusUpdateDto request) {
        var adminId = securityPrincipalProvider.getUserId();
        log.info("admin.order.status.update: orderId={}, event={}, admin={}", orderId, request.getEvent(), adminId);
        Order updated = statusTransitioner.transition(orderId, request.getEvent(), adminId, request.getReason());
        return ResponseEntity.ok(orderDtoConverter.toResponseDto(updated));
    }
}
