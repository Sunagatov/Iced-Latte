package com.zufar.icedlatte.order.endpoint;

import com.zufar.icedlatte.openapi.dto.OrderRequestDto;
import com.zufar.icedlatte.openapi.dto.OrderResponseDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.api.OrderManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = OrderEndpoint.ORDER_URL)
public class OrderEndpoint implements com.zufar.icedlatte.openapi.order.api.OrdersApi {

    public static final String ORDER_URL = "/api/v1/orders";

    private final OrderManager orderManager;

    @Override
    @PostMapping
    public ResponseEntity<OrderResponseDto> createNewOrder(final OrderRequestDto orderRequest) {
        log.info("Received orderRequest to add order.");
        var order = orderManager.createNewOrder(orderRequest);
        log.info("Order was added with id: {}", order.getId());
        return ResponseEntity.ok()
                .body(order);
    }

    @Override
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getOrders(final List<OrderStatus> statusList) {
        var status = statusList == null ? "Not provided" : statusList.stream().map(OrderStatus::toString).collect(Collectors.joining(", "));
        log.info("Received request to get all orders with status: {}.", status);
        var lisOfOrders = orderManager.getOrders(statusList);
        log.info("Orders retrieval processed.");
        return ResponseEntity.ok()
                .body(lisOfOrders);
    }
}
