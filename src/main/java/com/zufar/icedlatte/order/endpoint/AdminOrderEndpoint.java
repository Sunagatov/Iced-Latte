package com.zufar.icedlatte.order.endpoint;

import com.zufar.icedlatte.common.config.PaginationConfig;
import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.common.pagination.PageRequestFactory;
import com.zufar.icedlatte.openapi.dto.AdminOrderStatusUpdateDto;
import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderPageDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.openapi.order.api.AdminOrdersApi;
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
@RequestMapping(ApiPaths.ADMIN_ORDERS)
@SuppressWarnings("unused") // Spring MVC invokes endpoint methods via reflection.
public class AdminOrderEndpoint implements AdminOrdersApi {

    private final OrdersProvider ordersProvider;
    private final OrderStatusTransitioner statusTransitioner;
    private final OrderDtoConverter orderDtoConverter;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final PaginationConfig paginationConfig;

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    @GetMapping
    public ResponseEntity<OrderPageDto> getAllOrders(@RequestParam(required = false) final Integer page,
                                                     @RequestParam(required = false) final Integer size,
                                                     @RequestParam(required = false) final List<OrderStatus> status,
                                                     @RequestParam(required = false) final UUID userId,
                                                     @RequestParam(required = false) final String sortBy,
                                                     @RequestParam(required = false) final String sortDirection,
                                                     @RequestParam(required = false) final Integer year,
                                                     @RequestParam(required = false) final LocalDate dateFrom,
                                                     @RequestParam(required = false) final LocalDate dateTo) {
        PaginationConfig.Orders defaults = paginationConfig.orders();
        Pageable pageable = PageRequestFactory.of(
                page != null ? page : paginationConfig.defaultPageNumber(),
                size != null ? Math.min(size, defaults.maxPageSize()) : defaults.defaultPageSize(),
                sortBy != null ? sortBy : defaults.defaultSortAttribute(),
                sortDirection != null ? sortDirection : defaults.defaultSortDirection()
        );
        return ResponseEntity.ok(ordersProvider.getOrders(userId, status, year, dateFrom, dateTo, pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderDto> updateOrderStatus(@PathVariable final UUID orderId,
                                                      @Valid @RequestBody final AdminOrderStatusUpdateDto request) {
        var adminId = securityPrincipalProvider.getUserId();
        log.info("admin.order.status.update: orderId={}, event={}, admin={}", orderId, request.getEvent(), adminId);
        Order updated = statusTransitioner.transition(orderId, request.getEvent(), adminId, request.getReason());
        return ResponseEntity.ok(orderDtoConverter.toResponseDto(updated));
    }
}
