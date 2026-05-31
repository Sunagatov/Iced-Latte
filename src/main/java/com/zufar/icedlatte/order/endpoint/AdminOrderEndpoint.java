package com.zufar.icedlatte.order.endpoint;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.openapi.dto.AdminOrderStatusUpdateDto;
import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderPageDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.openapi.order.api.AdminOrdersApi;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.service.lifecycle.OrderStatusTransitioner;
import com.zufar.icedlatte.order.service.query.OrderDetailProvider;
import com.zufar.icedlatte.security.api.CurrentUserProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.ADMIN_ORDERS)
@SuppressWarnings("unused") // Spring MVC invokes endpoint methods via reflection.
public class AdminOrderEndpoint implements AdminOrdersApi {

    private final OrderDetailProvider orderDetailProvider;
    private final OrderStatusTransitioner statusTransitioner;
    private final OrderDtoConverter orderDtoConverter;
    private final CurrentUserProvider currentUserProvider;
    private final OrderPageRequestFactory orderPageRequestFactory;

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    @GetMapping
    public ResponseEntity<OrderPageDto> getAllOrders(
            @RequestParam(required = false) final Integer page,
            @RequestParam(required = false) final Integer size,
            @RequestParam(required = false) final List<OrderStatus> status,
            @RequestParam(required = false) final UUID userId,
            @RequestParam(required = false) final String sortBy,
            @RequestParam(required = false) final String sortDirection,
            @RequestParam(required = false) final Integer year,
            @RequestParam(required = false) final LocalDate dateFrom,
            @RequestParam(required = false) final LocalDate dateTo) {
        var pageable = orderPageRequestFactory.build(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(orderDetailProvider.getOrders(userId, status, year, dateFrom, dateTo, pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @PathVariable final UUID orderId, @Valid @RequestBody final AdminOrderStatusUpdateDto request) {
        var adminId = currentUserProvider.getUserId();
        log.info("admin.order.status.update: orderId={}, event={}, admin={}", orderId, request.getEvent(), adminId);
        Order updated = statusTransitioner.transition(orderId, request.getEvent(), adminId, request.getReason());
        return ResponseEntity.ok(orderDtoConverter.toResponseDto(updated));
    }
}
