package com.zufar.icedlatte.order.service;

import com.zufar.icedlatte.cart.api.CartCheckoutApi;
import com.zufar.icedlatte.cart.api.dto.AddCartItemRequest;
import com.zufar.icedlatte.openapi.dto.ReorderResponseDto;
import com.zufar.icedlatte.openapi.dto.UnavailableItemDto;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.entity.OrderItem;
import com.zufar.icedlatte.order.exception.OrderAccessDeniedException;
import com.zufar.icedlatte.order.exception.OrderNotFoundException;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.product.api.ProductCatalogApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderReorderService {

    private final OrderRepository orderRepository;
    private final ProductCatalogApi productCatalogApi;
    private final CartCheckoutApi cartCheckoutApi;

    @Transactional
    public ReorderResponseDto reorder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUserId().equals(userId)) {
            throw new OrderAccessDeniedException();
        }

        Set<AddCartItemRequest> itemsToAdd = new LinkedHashSet<>();
        List<UnavailableItemDto> unavailable = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            if (productCatalogApi.existsById(item.getProductId())) {
                itemsToAdd.add(new AddCartItemRequest(item.getProductId(), item.getProductsQuantity()));
            } else {
                unavailable.add(new UnavailableItemDto()
                        .productName(item.getProductName())
                        .reason("Product no longer available"));
            }
        }

        UUID cartId = null;
        if (!itemsToAdd.isEmpty()) {
            var cart = cartCheckoutApi.addItems(userId, itemsToAdd);
            cartId = cart.id();
        }

        log.info("order.reorder: orderId={}, added={}, unavailable={}", orderId, itemsToAdd.size(), unavailable.size());

        return new ReorderResponseDto()
                .cartId(cartId)
                .addedItems(itemsToAdd.size())
                .unavailableItems(unavailable);
    }
}
