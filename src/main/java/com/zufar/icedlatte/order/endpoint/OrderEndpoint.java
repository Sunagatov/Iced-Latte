package com.zufar.icedlatte.order.endpoint;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.openapi.dto.*;
import com.zufar.icedlatte.order.service.OrderCreator;
import com.zufar.icedlatte.order.service.OrderReorderService;
import com.zufar.icedlatte.order.service.lifecycle.OrderStatusTransitioner;
import com.zufar.icedlatte.order.service.query.OrderDetailProvider;
import com.zufar.icedlatte.security.api.CurrentUserProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(OrderEndpoint.ORDERS_URL)
@SuppressWarnings("unused") // Spring MVC invokes endpoint methods via reflection.
public class OrderEndpoint implements com.zufar.icedlatte.openapi.order.api.OrdersApi {

    public static final String ORDERS_URL = ApiPaths.ORDERS;

    private final CurrentUserProvider currentUserProvider;
    private final OrderDetailProvider orderDetailProvider;
    private final OrderCreator orderCreator;
    private final OrderStatusTransitioner orderStatusTransitioner;
    private final OrderReorderService orderReorderService;
    private final OrderPageRequestFactory orderPageRequestFactory;

    @Override
    @GetMapping
    public ResponseEntity<OrderPageDto> getOrders(
            @RequestParam(required = false) final Integer page,
            @RequestParam(required = false) final Integer size,
            @RequestParam(required = false) final String sortBy,
            @RequestParam(required = false) final String sortDirection,
            @RequestParam(required = false) final List<OrderStatus> status,
            @RequestParam(required = false) final Integer year,
            @RequestParam(required = false) final LocalDate dateFrom,
            @RequestParam(required = false) final LocalDate dateTo) {
        var userId = currentUserProvider.getUserId();
        var pageable = orderPageRequestFactory.build(page, size, sortBy, sortDirection);
        var result = orderDetailProvider.getOrders(userId, status, year, dateFrom, dateTo, pageable);
        return ResponseEntity.ok(result);
    }

    @Override
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable final UUID orderId) {
        var userId = currentUserProvider.getUserId();
        var order = orderDetailProvider.getOrder(orderId, userId);
        return ResponseEntity.ok(order);
    }

    @Override
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(
            @Valid @RequestBody final CreateNewOrderRequestDto request,
            @RequestHeader(value = "Idempotency-Key", required = false) final String idempotencyKey) {
        var userId = currentUserProvider.getUserId();
        log.info("orders.create: userId={}", userId);
        var order = orderCreator.create(userId, request, idempotencyKey);
        log.info("orders.created: userId={}, orderId={}", userId, order.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @Override
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderDto> cancelOrder(@PathVariable final UUID orderId) {
        var userId = currentUserProvider.getUserId();
        log.info("orders.cancel: userId={}, orderId={}", userId, orderId);
        var order = orderStatusTransitioner.cancel(orderId, userId);
        return ResponseEntity.ok(order);
    }

    @Override
    @PostMapping("/{orderId}/refund")
    public ResponseEntity<OrderDto> requestRefund(
            @PathVariable final UUID orderId, @Valid @RequestBody(required = false) final RefundRequestDto request) {
        var userId = currentUserProvider.getUserId();
        String reason = request != null ? request.getReason() : null;
        log.info("orders.refund: userId={}, orderId={}", userId, orderId);
        var order = orderStatusTransitioner.requestRefund(orderId, userId, reason);
        return ResponseEntity.ok(order);
    }

    @Override
    @PostMapping("/{orderId}/reorder")
    public ResponseEntity<ReorderResponseDto> reorder(@PathVariable final UUID orderId) {
        var userId = currentUserProvider.getUserId();
        log.info("orders.reorder: userId={}, orderId={}", userId, orderId);
        var result = orderReorderService.reorder(orderId, userId);
        return ResponseEntity.ok(result);
    }

    @Override
    @GetMapping("/{orderId}/history")
    public ResponseEntity<List<OrderStatusHistoryDto>> getOrderHistory(@PathVariable final UUID orderId) {
        var userId = currentUserProvider.getUserId();
        var history = orderDetailProvider.getOrderHistory(orderId, userId);
        return ResponseEntity.ok(history);
    }
}
