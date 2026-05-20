package com.zufar.icedlatte.cart.converter;

import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartItemDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ShoppingCartDtoConverter {

    public ShoppingCartDto toDto(final ShoppingCart cart,
                                 final Map<UUID, ProductInfoDto> productsById) {
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
                .productsQuantity(productsQuantity);
    }

    private ShoppingCartItemDto toItemDto(ShoppingCartItem item,
                                          ProductInfoDto productInfo) {
        return new ShoppingCartItemDto()
                .id(item.getId())
                .productInfo(productInfo)
                .productQuantity(item.getProductQuantity());
    }
}
