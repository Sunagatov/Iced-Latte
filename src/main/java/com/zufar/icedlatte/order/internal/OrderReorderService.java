package com.zufar.icedlatte.order.internal;

import com.zufar.icedlatte.cart.api.CartCheckoutApi;
import com.zufar.icedlatte.openapi.dto.NewShoppingCartItemDto;
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

        Set<NewShoppingCartItemDto> itemsToAdd = new LinkedHashSet<>();
        List<UnavailableItemDto> unavailable = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            if (productCatalogApi.existsById(item.getProductId())) {
                var cartItem = new NewShoppingCartItemDto();
                cartItem.setProductId(item.getProductId());
                cartItem.setProductQuantity(item.getProductsQuantity());
                itemsToAdd.add(cartItem);
            } else {
                unavailable.add(new UnavailableItemDto()
                        .productName(item.getProductName())
                        .reason("Product no longer available"));
            }
        }

        UUID cartId = null;
        if (!itemsToAdd.isEmpty()) {
            var cart = cartCheckoutApi.addItems(userId, itemsToAdd);
            cartId = cart.getId();
        }

        log.info("order.reorder: orderId={}, added={}, unavailable={}", orderId, itemsToAdd.size(), unavailable.size());

        return new ReorderResponseDto()
                .cartId(cartId)
                .addedItems(itemsToAdd.size())
                .unavailableItems(unavailable);
    }
}
