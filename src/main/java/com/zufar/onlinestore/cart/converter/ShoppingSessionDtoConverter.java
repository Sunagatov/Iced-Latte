package com.zufar.onlinestore.cart.converter;

import com.zufar.onlinestore.cart.api.ItemsTotalPriceCalculator;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.dto.ShoppingSessionItemDto;
import com.zufar.onlinestore.cart.entity.ShoppingSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;

@Component
@RequiredArgsConstructor
public class ShoppingSessionDtoConverter {

    private final ShoppingSessionItemDtoConverter shoppingSessionItemDtoConverter;
    private final ItemsTotalPriceCalculator itemsTotalPriceCalculator;

    public ShoppingSessionDto toDto(final ShoppingSession entity) {
        Collection<ShoppingSessionItemDto> items = entity.getItems().stream()
                .map(shoppingSessionItemDtoConverter::toDto)
                .toList();

        BigDecimal itemsTotalPrice = itemsTotalPriceCalculator.calculate(items);

        return new ShoppingSessionDto(
                entity.getId(),
                entity.getUserId(),
                items,
                entity.getItemsQuantity(),
                itemsTotalPrice,
                entity.getProductsQuantity(),
                entity.getCreatedAt(),
                entity.getClosedAt()
        );
    }
}