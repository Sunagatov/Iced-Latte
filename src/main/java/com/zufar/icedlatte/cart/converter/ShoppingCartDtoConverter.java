package com.zufar.icedlatte.cart.converter;

import com.zufar.icedlatte.cart.api.dto.CartItemSnapshot;
import com.zufar.icedlatte.cart.api.dto.CartSnapshot;
import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartItemDto;
import com.zufar.icedlatte.product.api.dto.ProductSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ShoppingCartDtoConverter {

    public ShoppingCartDto toDto(final ShoppingCart cart,
                                 final Map<UUID, ProductSnapshot> productsById) {
        List<ShoppingCartItemDto> itemDtos = cart.getItems() == null ? List.of() :
                cart.getItems().stream()
                        .map(item -> toItemDto(item, productsById.get(item.getProductId())))
                        .toList();

        BigDecimal itemsTotalPrice = itemDtos.stream()
                .map(item -> item.getProductInfo().getPrice().multiply(BigDecimal.valueOf(item.getProductQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int itemsQuantity = itemDtos.size();
        int productsQuantity = itemDtos.stream().mapToInt(ShoppingCartItemDto::getProductQuantity).sum();

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

    private ShoppingCartItemDto toItemDto(ShoppingCartItem item,
                                          ProductSnapshot productInfo) {
        return new ShoppingCartItemDto()
                .id(item.getId())
                .productInfo(toDto(productInfo))
                .productQuantity(item.getProductQuantity());
    }

    private ProductInfoDto toDto(ProductSnapshot product) {
        return new ProductInfoDto()
                .id(product.id())
                .name(product.name())
                .price(product.price())
                .productFileUrl(product.productFileUrl());
    }

    public CartSnapshot toSnapshot(final ShoppingCart cart,
                                   final Map<UUID, ProductSnapshot> productsById) {
        List<CartItemSnapshot> items = cart.getItems() == null ? List.of() :
                cart.getItems().stream()
                        .map(item -> new CartItemSnapshot(
                                item.getId(),
                                productsById.get(item.getProductId()),
                                item.getProductQuantity()))
                        .toList();

        BigDecimal totalPrice = items.stream()
                .filter(item -> item.product() != null && item.product().price() != null)
                .map(item -> item.product().price().multiply(BigDecimal.valueOf(item.productQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int productsQuantity = items.stream().mapToInt(CartItemSnapshot::productQuantity).sum();

        return new CartSnapshot(cart.getId(), cart.getUserId(), items, items.size(),
                totalPrice, productsQuantity, cart.getCreatedAt(), cart.getClosedAt());
    }
}
