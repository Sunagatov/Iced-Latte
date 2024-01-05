package com.zufar.icedlatte.order.endpoint;

import com.zufar.icedlatte.openapi.dto.AddNewItemsToShoppingCartRequest;
import com.zufar.icedlatte.openapi.dto.NewShoppingCartItemDto;
import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(value = OrderEndpoint.ORDER_URL)
public class OrderEndpoint implements com.zufar.icedlatte.openapi.order.api.OrderApi {

    public static final String ORDER_URL = "/api/v1/orders";

    private final SecurityPrincipalProvider securityPrincipalProvider;

    @Override
    @PostMapping
    public ResponseEntity<Void> addOrder(@RequestBody final OrderDto request) {
        UUID userId = securityPrincipalProvider.getUserId();
        log.warn("Received the request to add a new order for user with id: {}", userId);
        log.info("Order was added with id: {}");
        return ResponseEntity.ok().build();
    }
}
