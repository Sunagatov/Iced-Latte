package com.zufar.icedlatte.order.endpoint;

import com.zufar.icedlatte.common.config.PaginationConfig;
import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.common.pagination.PageRequestFactory;
import com.zufar.icedlatte.openapi.dto.CreateNewOrderRequestDto;
import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderPageDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.openapi.dto.RefundRequestDto;
import com.zufar.icedlatte.openapi.dto.ReorderResponseDto;
import com.zufar.icedlatte.openapi.dto.OrderStatusHistoryDto;
import com.zufar.icedlatte.order.api.OrderCreator;
import com.zufar.icedlatte.order.api.OrderDetailProvider;
import com.zufar.icedlatte.order.api.OrderLifecycleService;
import com.zufar.icedlatte.order.api.OrderReorderService;
import com.zufar.icedlatte.order.api.OrdersProvider;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(OrderEndpoint.ORDERS_URL)
@SuppressWarnings("unused") // Spring MVC invokes endpoint methods via reflection.
public class OrderEndpoint implements com.zufar.icedlatte.openapi.order.api.OrdersApi {

    public static final String ORDERS_URL = ApiPaths.ORDERS;

    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final OrdersProvider ordersProvider;
    private final OrderDetailProvider orderDetailProvider;
    private final OrderCreator orderCreator;
    private final OrderLifecycleService orderLifecycleService;
    private final OrderReorderService orderReorderService;
    private final PaginationConfig paginationConfig;

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
        var userId = securityPrincipalProvider.getUserId();
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

    @Override
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable final UUID orderId) {
        var userId = securityPrincipalProvider.getUserId();
        var order = orderDetailProvider.getOrder(orderId, userId);
        return ResponseEntity.ok(order);
    }

    @Override
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(
            @Valid @RequestBody final CreateNewOrderRequestDto request,
            @RequestHeader(value = "Idempotency-Key", required = false) final String idempotencyKey) {
        var userId = securityPrincipalProvider.getUserId();
        log.info("orders.create: userId={}", userId);
        var order = orderCreator.create(userId, request, idempotencyKey);
        log.info("orders.created: userId={}, orderId={}", userId, order.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @Override
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderDto> cancelOrder(@PathVariable final UUID orderId) {
        var userId = securityPrincipalProvider.getUserId();
        log.info("orders.cancel: userId={}, orderId={}", userId, orderId);
        var order = orderLifecycleService.cancel(orderId, userId);
        return ResponseEntity.ok(order);
    }

    @Override
    @PostMapping("/{orderId}/refund")
    public ResponseEntity<OrderDto> requestRefund(
            @PathVariable final UUID orderId,
            @Valid @RequestBody(required = false) final RefundRequestDto request) {
        var userId = securityPrincipalProvider.getUserId();
        String reason = request != null ? request.getReason() : null;
        log.info("orders.refund: userId={}, orderId={}", userId, orderId);
        var order = orderLifecycleService.requestRefund(orderId, userId, reason);
        return ResponseEntity.ok(order);
    }

    @Override
    @PostMapping("/{orderId}/reorder")
    public ResponseEntity<ReorderResponseDto> reorder(@PathVariable final UUID orderId) {
        var userId = securityPrincipalProvider.getUserId();
        log.info("orders.reorder: userId={}, orderId={}", userId, orderId);
        var result = orderReorderService.reorder(orderId, userId);
        return ResponseEntity.ok(result);
    }

    @Override
    @GetMapping("/{orderId}/history")
    public ResponseEntity<List<OrderStatusHistoryDto>> getOrderHistory(@PathVariable final UUID orderId) {
        var userId = securityPrincipalProvider.getUserId();
        var history = orderDetailProvider.getOrderHistory(orderId, userId);
        return ResponseEntity.ok(history);
    }
}
