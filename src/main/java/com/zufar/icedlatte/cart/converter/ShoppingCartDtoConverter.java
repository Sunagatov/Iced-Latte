package com.zufar.icedlatte.cart.converter;

import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartItemDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ShoppingCartDtoConverter {
    private ShoppingCartItemDtoConverter shoppingCartItemDtoConverter;

    public ShoppingCartDto toDto(final ShoppingCart entity) {
        List<ShoppingCartItemDto> shoppingCartItemDto = toDtoList(entity.getItems());
        return new ShoppingCartDto(
                entity.getId(),
                entity.getUserId(),
                shoppingCartItemDto,
                entity.getItemsQuantity(),
                calculateItemsTotalPrice(shoppingCartItemDto),
                entity.getProductsQuantity(),
                entity.getClosedAt()
        );
    }

    private List<ShoppingCartItemDto> toDtoList(Set<ShoppingCartItem> items) {
        return items.stream()
                .map(shoppingCartItemDtoConverter::toDto)
                .collect(Collectors.toList());
    }

    private BigDecimal calculateItemsTotalPrice(List<ShoppingCartItemDto> shoppingCartItemDto) {
        return shoppingCartItemDto.stream()
                .map(item -> item.getProductInfo()
                        .getPrice()
                        .multiply(BigDecimal.valueOf(item.getProductQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
