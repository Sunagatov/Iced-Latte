package com.zufar.icedlatte.order.endpoint;

import com.zufar.icedlatte.openapi.dto.AddedOrder;
import com.zufar.icedlatte.openapi.dto.ListOfAddedOrders;
import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.api.OrderApi;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = OrderEndpoint.ORDER_URL)
public class OrderEndpoint {

    public static final String ORDER_URL = "/api/v1/orders";

    private final OrderApi orderApi;

    @PostMapping
    public ResponseEntity<AddedOrder> addOrder(@RequestBody @Valid final OrderDto request) {
        log.info("Received request to add order.");
        var addedOrder = orderApi.addNewOrder(request);
        log.info("Order was added with id: {}", addedOrder.getId());
        return ResponseEntity.ok().body(addedOrder);
    }

    @GetMapping
    public ResponseEntity<ListOfAddedOrders> getListOfOrdersForUser(@RequestParam(value = "status", required = false) final Set<OrderStatus> statusList) {
        var status = statusList == null ? "Not provided" : statusList.stream().map(OrderStatus::toString).collect(Collectors.joining(", "));
        log.info("Received request to get all orders with status: {}.", status);
        var lisOfOrders = orderApi.getAllOrders(statusList);
        log.info("Orders retrieval processed.");
        return ResponseEntity.ok().body(lisOfOrders);
    }
}
