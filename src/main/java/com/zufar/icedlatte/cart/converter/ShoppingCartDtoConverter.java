package com.zufar.icedlatte.cart.converter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.zufar.icedlatte.cart.api.dto.CartItemSnapshot;
import com.zufar.icedlatte.cart.api.dto.CartSnapshot;
import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartItemDto;
import com.zufar.icedlatte.product.api.dto.ProductSnapshot;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ShoppingCartDtoConverter {

    private final ProductInfoDtoConverter productInfoDtoConverter;

    public ShoppingCartDto toDto(final ShoppingCart cart, final Map<UUID, ProductSnapshot> productsById) {
        List<ShoppingCartItemDto> itemDtos = cart.getItems() == null
                ? List.of()
                : cart.getItems().stream()
                        .filter(item -> productsById.containsKey(item.getProductId()))
                        .map(item -> {
                            var product = Objects.requireNonNull(productsById.get(item.getProductId()));
                            return toItemDto(item, product);
                        })
                        .toList();

        BigDecimal itemsTotalPrice = itemDtos.stream()
                .map(item -> item.getProductInfo().getPrice().multiply(BigDecimal.valueOf(item.getProductQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int itemsQuantity = itemDtos.size();
        int productsQuantity = itemDtos.stream()
                .mapToInt(ShoppingCartItemDto::getProductQuantity)
                .sum();

        return new ShoppingCartDto()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(itemDtos)
                .itemsTotalPrice(itemsTotalPrice)
                .itemsQuantity(itemsQuantity)
                .productsQuantity(productsQuantity)
                .createdAt(cart.getCreatedAt())
                .closedAt(cart.getClosedAt());
    }

    private ShoppingCartItemDto toItemDto(ShoppingCartItem item, ProductSnapshot productInfo) {
        return new ShoppingCartItemDto()
                .id(item.getId())
                .productInfo(productInfoDtoConverter.toSummaryDto(productInfo))
                .productQuantity(item.getProductQuantity());
    }

    public CartSnapshot toSnapshot(final ShoppingCart cart, final Map<UUID, ProductSnapshot> productsById) {
        List<CartItemSnapshot> items = cart.getItems() == null
                ? List.of()
                : cart.getItems().stream()
                        .filter(item -> productsById.containsKey(item.getProductId()))
                        .map(item -> {
                            var product = Objects.requireNonNull(productsById.get(item.getProductId()));
                            return new CartItemSnapshot(item.getId(), product, item.getProductQuantity());
                        })
                        .toList();

        BigDecimal totalPrice = items.stream()
                .map(item -> item.product().price().multiply(BigDecimal.valueOf(item.productQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int productsQuantity =
                items.stream().mapToInt(CartItemSnapshot::productQuantity).sum();

        return new CartSnapshot(
                cart.getId(),
                cart.getUserId(),
                items,
                items.size(),
                totalPrice,
                productsQuantity,
                cart.getCreatedAt(),
                cart.getClosedAt());
    }
}
